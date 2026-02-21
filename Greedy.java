[file name]: Greedy.java
[file content begin]
import java.util.*;

public class Greedy {

    // Depth of lookahead for Hard mode
    private static final int HARD_DEPTH = 4;

    public static Direction choose(BoardModel m, Difficulty level) {
        switch (level) {
            case EASY:   return playEasyWithDivideConquer(m);
            case MEDIUM: return playMediumWithDivideConquer(m);
            case HARD:   return playHardWithDivideConquer(m);
            default:     return playMediumWithDivideConquer(m);
        }
    }

    /**
     * EASY MODE with Divide & Conquer:
     * 1. Divide board into quadrants around CPU position
     * 2. Evaluate each quadrant separately
     * 3. Combine results to choose best direction
     */
    private static Direction playEasyWithDivideConquer(BoardModel m) {
        // Divide: Get directions that lead to different board regions
        List<Direction> allDirections = Arrays.asList(Direction.values());
        
        // Conquer: Evaluate each direction using simple greedy logic
        Map<Direction, Integer> directionScores = new HashMap<>();
        
        for (Direction d : allDirections) {
            int score = evaluateEasyDirection(m, d);
            directionScores.put(d, score);
        }
        
        // Combine: Choose the best direction
        Direction bestDir = null;
        int bestScore = -1;
        
        for (Map.Entry<Direction, Integer> entry : directionScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestDir = entry.getKey();
            }
        }
        
        // Fallback to original easy mode if no good move found
        if (bestDir == null || bestScore <= 0) {
            return playEasyOriginal(m);
        }
        
        return bestDir;
    }
    
    private static int evaluateEasyDirection(BoardModel m, Direction d) {
        BoardModel.SlideResult res = m.slide(m.cpuRow, m.cpuCol, d, false);
        
        // Safety check
        boolean dead = res.hitMine && (m.cpuShields == 0);
        boolean moved = (res.r != m.cpuRow || res.c != m.cpuCol);
        
        if (dead || !moved) {
            return -1000; // Penalize dangerous or useless moves
        }
        
        // Score based on immediate rewards
        int score = 0;
        score += res.gems * 100;     // Gems are valuable
        score += res.shields * 50;    // Shields are valuable but less than gems
        
        // Bonus for moving into new areas (exploration)
        if (res.gems == 0 && res.shields == 0) {
            score += 10; // Small reward for exploration
        }
        
        return score;
    }

    /**
     * MEDIUM MODE with Divide & Conquer:
     * Uses BFS but divides search space into promising regions first
     */
    private static Direction playMediumWithDivideConquer(BoardModel m) {
        // Divide: Identify promising regions with gems
        List<Direction> promisingDirections = getPromisingDirections(m);
        
        if (!promisingDirections.isEmpty()) {
            // Conquer: Use BFS on promising directions only
            return bfsToGem(m, promisingDirections);
        }
        
        // If no promising directions, use full BFS
        return playMediumOriginal(m);
    }
    
    private static List<Direction> getPromisingDirections(BoardModel m) {
        List<Direction> promising = new ArrayList<>();
        
        for (Direction d : Direction.values()) {
            BoardModel.SlideResult res = m.slide(m.cpuRow, m.cpuCol, d, false);
            
            // Check if direction leads to area with potential
            if (leadsToPromisingArea(m, res.r, res.c, d)) {
                promising.add(d);
            }
        }
        
        return promising;
    }
    
    private static boolean leadsToPromisingArea(BoardModel m, int startR, int startC, Direction dir) {
        // Quick heuristic: check if this direction eventually leads to gems
        // by scanning a few steps ahead
        int r = startR;
        int c = startC;
        
        for (int i = 0; i < 3; i++) {
            r += dir.dx;
            c += dir.dy;
            
            if (!m.inBounds(r, c) || m.grid[r][c].wall) {
                break;
            }
            
            if (m.grid[r][c].gem) {
                return true;
            }
            
            if (m.grid[r][c].stop || m.grid[r][c].mine) {
                break;
            }
        }
        
        return false;
    }
    
    private static Direction bfsToGem(BoardModel m, List<Direction> startDirections) {
        Queue<Node> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        // Start BFS from multiple promising directions
        for (Direction startDir : startDirections) {
            BoardModel.SlideResult firstStep = m.slide(m.cpuRow, m.cpuCol, startDir, false);
            
            // Safety check
            if (firstStep.hitMine && m.cpuShields == 0) {
                continue;
            }
            
            int startShields = m.cpuShields + firstStep.shields;
            if (firstStep.hitMine) {
                startShields--; // Use shield
            }
            
            Node startNode = new Node(firstStep.r, firstStep.c, startShields, startDir);
            queue.add(startNode);
            visited.add(startNode.key());
            
            // If first step already collects a gem, return it
            if (firstStep.gems > 0) {
                return startDir;
            }
        }
        
        // Continue BFS
        while (!queue.isEmpty()) {
            Node curr = queue.poll();
            
            for (Direction d : Direction.values()) {
                BoardModel.SlideResult res = m.slide(curr.r, curr.c, d, false);
                
                int nextShields = curr.shields + res.shields;
                boolean isSafe = true;
                
                if (res.hitMine) {
                    if (nextShields > 0) {
                        nextShields--;
                    } else {
                        isSafe = false;
                    }
                }
                
                if (!isSafe) continue;
                if (res.r == curr.r && res.c == curr.c && res.gems == 0) continue;
                
                Direction firstMove = curr.firstDir;
                
                if (res.gems > 0) {
                    return firstMove;
                }
                
                Node nextNode = new Node(res.r, res.c, nextShields, firstMove);
                if (!visited.contains(nextNode.key())) {
                    visited.add(nextNode.key());
                    queue.add(nextNode);
                }
            }
        }
        
        // Fallback to original if BFS on promising directions fails
        return playMediumOriginal(m);
    }

    /**
     * HARD MODE with Divide & Conquer:
     * Divides the lookahead search into subproblems
     */
    private static Direction playHardWithDivideConquer(BoardModel m) {
        // Divide: Cluster potential targets (gems and shields)
        List<TargetCluster> clusters = clusterTargets(m);
        
        if (clusters.isEmpty()) {
            return playHardOriginal(m);
        }
        
        // Evaluate each cluster independently
        Direction bestDir = null;
        double bestScore = -Double.MAX_VALUE;
        
        for (TargetCluster cluster : clusters) {
            // Conquer: Evaluate paths to this cluster
            Direction clusterDir = evaluateCluster(m, cluster);
            
            if (clusterDir != null) {
                // Simulate the move and evaluate its quality
                BoardModel.SlideResult res = m.slide(m.cpuRow, m.cpuCol, clusterDir, false);
                double score = evaluateMoveScore(m, res, clusterDir, HARD_DEPTH);
                
                if (score > bestScore) {
                    bestScore = score;
                    bestDir = clusterDir;
                }
            }
        }
        
        if (bestDir != null) {
            return bestDir;
        }
        
        // Fallback to original hard mode
        return playHardOriginal(m);
    }
    
    private static List<TargetCluster> clusterTargets(BoardModel m) {
        List<TargetCluster> clusters = new ArrayList<>();
        
        // Find all gems and shields on the board
        List<int[]> targets = new ArrayList<>();
        for (int r = 0; r < m.rows; r++) {
            for (int c = 0; c < m.cols; c++) {
                if (m.grid[r][c].gem || m.grid[r][c].shield) {
                    targets.add(new int[]{r, c});
                }
            }
        }
        
        // Simple clustering by quadrant relative to CPU position
        Map<String, TargetCluster> quadrantClusters = new HashMap<>();
        String[] quadrants = {"NW", "NE", "SW", "SE"};
        
        for (String quad : quadrants) {
            quadrantClusters.put(quad, new TargetCluster(quad));
        }
        
        // Assign targets to quadrants
        for (int[] target : targets) {
            int r = target[0];
            int c = target[1];
            
            String quad;
            if (r < m.cpuRow) {
                quad = (c < m.cpuCol) ? "NW" : "NE";
            } else {
                quad = (c < m.cpuCol) ? "SW" : "SE";
            }
            
            quadrantClusters.get(quad).addTarget(target[0], target[1]);
        }
        
        // Return only non-empty clusters
        for (TargetCluster cluster : quadrantClusters.values()) {
            if (!cluster.isEmpty()) {
                clusters.add(cluster);
            }
        }
        
        return clusters;
    }
    
    private static Direction evaluateCluster(BoardModel m, TargetCluster cluster) {
        // Find the best direction to approach this cluster
        double bestScore = -Double.MAX_VALUE;
        Direction bestDir = null;
        
        for (Direction d : Direction.values()) {
            BoardModel.SlideResult res = m.slide(m.cpuRow, m.cpuCol, d, false);
            
            // Safety check
            if (res.hitMine && m.cpuShields == 0) {
                continue;
            }
            
            // Calculate how good this direction is for reaching the cluster
            double clusterScore = cluster.evaluateDirection(res.r, res.c, d);
            
            // Combine with immediate rewards
            double score = clusterScore + (res.gems * 100.0) + (res.shields * 10.0);
            
            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }
        
        return bestDir;
    }
    
    private static double evaluateMoveScore(BoardModel m, BoardModel.SlideResult res, Direction dir, int depth) {
        if (depth == 0) return 0;
        
        int currentShields = m.cpuShields + res.shields;
        if (res.hitMine) {
            currentShields--;
        }
        
        double score = (res.gems * 100.0) + (res.shields * 10.0);
        
        // Recursive lookahead with reduced branching factor
        Set<Integer> visited = new HashSet<>();
        visited.add(res.r * 1000 + res.c);
        
        // Only explore promising directions (divide the search space)
        List<Direction> promisingDirs = getPromisingDirectionsFromPosition(m, res.r, res.c);
        
        double futureScore = 0;
        for (Direction nextDir : promisingDirs) {
            BoardModel.SlideResult nextRes = m.slide(res.r, res.c, nextDir, false);
            
            int cellKey = nextRes.r * 1000 + nextRes.c;
            if (visited.contains(cellKey) && nextRes.gems == 0) continue;
            
            double nextScore = evaluateMoveScore(m, nextRes, nextDir, depth - 1);
            futureScore = Math.max(futureScore, nextScore);
        }
        
        return score + futureScore * 0.9;
    }
    
    private static List<Direction> getPromisingDirectionsFromPosition(BoardModel m, int r, int c) {
        List<Direction> promising = new ArrayList<>();
        
        // Simple heuristic: directions that lead to visible items
        for (Direction d : Direction.values()) {
            int nr = r + d.dx;
            int nc = c + d.dy;
            
            if (m.inBounds(nr, nc) && !m.grid[nr][nc].wall) {
                // Check a few steps ahead
                for (int i = 0; i < 2; i++) {
                    if (m.grid[nr][nc].gem || m.grid[nr][nc].shield) {
                        promising.add(d);
                        break;
                    }
                    nr += d.dx;
                    nc += d.dy;
                    if (!m.inBounds(nr, nc) || m.grid[nr][nc].wall) break;
                }
            }
        }
        
        // If no promising directions found, return all safe directions
        if (promising.isEmpty()) {
            for (Direction d : Direction.values()) {
                BoardModel.SlideResult res = m.slide(r, c, d, false);
                if (!res.hitMine && (res.r != r || res.c != c)) {
                    promising.add(d);
                }
            }
        }
        
        return promising;
    }

    // ORIGINAL IMPLEMENTATIONS (kept as fallbacks)
    
    private static Direction playEasyOriginal(BoardModel m) {
        List<Direction> safeMoves = new ArrayList<>();
        List<Direction> itemMoves = new ArrayList<>();

        for (Direction d : Direction.values()) {
            BoardModel.SlideResult res = m.slide(m.cpuRow, m.cpuCol, d, false);

            boolean dead = res.hitMine && (m.cpuShields == 0);
            boolean moved = (res.r != m.cpuRow || res.c != m.cpuCol);

            if (!dead && moved) {
                safeMoves.add(d);
                if (res.gems > 0 || res.shields > 0) {
                    itemMoves.add(d);
                }
            }
        }

        if (!itemMoves.isEmpty()) {
            return itemMoves.get(new Random().nextInt(itemMoves.size()));
        }

        if (!safeMoves.isEmpty()) {
            return safeMoves.get(new Random().nextInt(safeMoves.size()));
        }

        return null;
    }

    private static Direction playMediumOriginal(BoardModel m) {
        Queue<Node> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        Node start = new Node(m.cpuRow, m.cpuCol, m.cpuShields, null);
        queue.add(start);
        visited.add(start.key());

        while (!queue.isEmpty()) {
            Node curr = queue.poll();

            for (Direction d : Direction.values()) {
                BoardModel.SlideResult res = m.slide(curr.r, curr.c, d, false);

                int nextShields = curr.shields + res.shields;
                boolean isSafe = true;

                if (res.hitMine) {
                    if (nextShields > 0) {
                        nextShields--;
                    } else {
                        isSafe = false;
                    }
                }

                if (!isSafe) continue;
                if (res.r == curr.r && res.c == curr.c && res.gems == 0) continue;

                Direction firstMove = (curr.firstDir == null) ? d : curr.firstDir;

                if (res.gems > 0) {
                    return firstMove;
                }

                Node nextNode = new Node(res.r, res.c, nextShields, firstMove);
                if (!visited.contains(nextNode.key())) {
                    visited.add(nextNode.key());
                    queue.add(nextNode);
                }
            }
        }

        return playEasyOriginal(m);
    }

    private static Direction playHardOriginal(BoardModel m) {
        double bestScore = -Double.MAX_VALUE;
        Direction bestDir = null;

        for (Direction d : Direction.values()) {
            BoardModel.SlideResult res = m.slide(m.cpuRow, m.cpuCol, d, false);
            
            int currentShields = m.cpuShields + res.shields;
            boolean dead = false;

            if (res.hitMine) {
                if (currentShields > 0) {
                    currentShields--;
                } else {
                    dead = true;
                }
            }

            if (dead) continue;
            
            if (res.r == m.cpuRow && res.c == m.cpuCol && res.gems == 0) continue;

            double score = (res.gems * 100.0) + (res.shields * 10.0);
            
            Set<Integer> visitedCells = new HashSet<>();
            visitedCells.add(res.r * 1000 + res.c);
            
            score += getRecursiveScore(m, res.r, res.c, currentShields, HARD_DEPTH - 1, visitedCells);

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }

        if (bestDir != null) return bestDir;
        return playEasyOriginal(m);
    }

    private static double getRecursiveScore(BoardModel m, int startR, int startC, int startShields, int depth, Set<Integer> visited) {
        if (depth == 0) return 0;

        double maxScore = 0;

        for (Direction d : Direction.values()) {
            BoardModel.SlideResult res = m.slide(startR, startC, d, false);
            
            int nextShields = startShields + res.shields;
            boolean dead = false;

            if (res.hitMine) {
                if (nextShields > 0) nextShields--;
                else dead = true;
            }

            if (dead) continue;
            
            int cellKey = res.r * 1000 + res.c;
            if (visited.contains(cellKey) && res.gems == 0) continue;

            double currentMoveScore = (res.gems * 100.0) + (res.shields * 10.0);
            
            Set<Integer> nextVisited = new HashSet<>(visited);
            nextVisited.add(cellKey);

            double futureScore = getRecursiveScore(m, res.r, res.c, nextShields, depth - 1, nextVisited);

            maxScore = Math.max(maxScore, currentMoveScore + futureScore * 0.9);
        }
        return maxScore;
    }

    // Helper Node for BFS
    private static class Node {
        int r, c, shields;
        Direction firstDir;
        Node(int r, int c, int s, Direction d) { 
            this.r = r; this.c = c; this.shields = s; this.firstDir = d; 
        }
        String key() { return r + "," + c + "," + shields; }
    }
    
    // Helper class for target clustering
    private static class TargetCluster {
        String quadrant;
        List<int[]> targets;
        
        TargetCluster(String quad) {
            this.quadrant = quad;
            this.targets = new ArrayList<>();
        }
        
        void addTarget(int r, int c) {
            targets.add(new int[]{r, c});
        }
        
        boolean isEmpty() {
            return targets.isEmpty();
        }
        
        double evaluateDirection(int r, int c, Direction d) {
            if (targets.isEmpty()) return 0;
            
            // Calculate average distance to targets
            double totalDistance = 0;
            for (int[] target : targets) {
                double dist = Math.sqrt(Math.pow(target[0] - r, 2) + Math.pow(target[1] - c, 2));
                totalDistance += dist;
            }
            
            double avgDistance = totalDistance / targets.size();
            
            // Direction vector towards cluster center
            int[] center = getCenter();
            int dx = Integer.compare(center[0] - r, 0);
            int dy = Integer.compare(center[1] - c, 0);
            
            // Score based on alignment with cluster direction
            double alignment = 0;
            if (d.dx == dx && d.dy == dy) {
                alignment = 50; // Perfect alignment
            } else if (d.dx == dx || d.dy == dy) {
                alignment = 25; // Partial alignment
            }
            
            // Combine distance and alignment
            return (100.0 / avgDistance) + alignment;
        }
        
        private int[] getCenter() {
            if (targets.isEmpty()) return new int[]{0, 0};
            
            int totalR = 0, totalC = 0;
            for (int[] target : targets) {
                totalR += target[0];
                totalC += target[1];
            }
            
            return new int[]{totalR / targets.size(), totalC / targets.size()};
        }
    }
}
[file content end]