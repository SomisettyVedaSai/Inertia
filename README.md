# Inertia-Based Grid Puzzle Game (Java)

This project is a Java-based grid puzzle game that simulates **inertia-driven movement**.  
Once a direction is chosen, the player continues moving in that direction until a boundary or obstacle is reached.  
The game combines logical decision-making with an interactive graphical interface.

---

## 🎮 Game Overview

- The game is played on a 2D grid
- Movement is allowed in four directions: Up, Down, Left, Right
- Movement does not stop immediately after one step
- The player slides continuously until blocked
- The goal is to reach the target cell in the fewest moves possible

---

## 🧩 Key Features

- Grid-based movement with inertia behavior  
- Direction-controlled navigation  
- Automatic decision-making logic for movement  
- Multiple difficulty levels  
- Graphical user interface using Java Swing  
- Clear separation between logic and visualization  

---

## 🛠️ Technologies Used

- **Language:** Java  
- **GUI:** Java Swing  

---

## 📂 Project Structure

```
Inertia-Game/
│
├── BoardModel.java # Manages grid state and movement rules
├── Cell.java # Represents a single grid cell
├── Difficulty.java # Defines game difficulty levels
├── Direction.java # Allowed movement directions
├── Greedy.java # Movement decision logic
├── GridPanel.java # Renders the grid and movement
├── InertiaGameFrame.java # Main application window
└── README.md # Project documentation
```
---

## 🔍 File Description

### BoardModel.java
Handles the grid layout, player position, obstacles, and movement constraints.

### Cell.java
Defines properties of individual cells such as position and state.

### Direction.java
Specifies valid movement directions used throughout the game.

### Difficulty.java
Controls grid size and obstacle complexity.

### Greedy.java
Determines the next movement direction based on the current game state.

### GridPanel.java
Displays the grid visually and updates the view after each move.

### InertiaGameFrame.java
Acts as the main entry point and connects all components.

---

## ▶️ How to Run

### Requirements
- Java JDK 8 or above

### Steps

```bash
javac *.java
java InertiaGameFrame


---

🧪 Behavior Summary

Each move selects a direction

The player continues moving in that direction until stopped

Decisions are made step-by-step based on the current position

The game ends when the target is reached

🚀 Possible Improvements

Add path visualization

Include step counter and score display

Add restart and reset options

Introduce additional movement rules

Enhance visual effects

👤 Author

Somisetty Veda Sai
Computer Science

---
