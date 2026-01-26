#  Skyscraper Stack Builder Game

##  Project Overview
**Skyscraper Stack Builder** is a Java-based game developed as part of our **Data Structures semester project**.  
The game is inspired by a Q-Mobile block-stacking game and demonstrates the **practical implementation of the Stack data structure** in a fun, interactive way.


------------------------
This project was developed by:
- **Umam Zahra** – [GitHub](https://github.com/your-Zahraoi)
- **Zuneera Tariq** – [GitHub](https://github.com/zuneeratariq13-code)


 **Achievement:**  
This project was presented in an **Intersubject Competition**, where it won **Third Prize**.

---

## Game Description
- The player controls a **horizontally moving block**
- The goal is to **place each block precisely** on top of the previous one
- Each successful placement **increases the score**
- As the game progresses, the **speed of movement increases**
- If a block does **not overlap** with the existing stack, the game ends
- At game over, a **scoreboard displays the top three high scores** along with player names

---

## Data Structures & Concepts Used

### Stack Data Structure
- Used to store and manage each successfully placed block
- Follows **Last-In-First-Out (LIFO)** behavior
- Helps in tracking and rendering the current stack of blocks

### Game State Management
- Controls different states such as:
  - Active gameplay
  - Score updates
  - Game termination

### Event Handling
- Manages user input
- Controls block placement timing

### Loops & Conditional Logic
- Continuous block movement
- Collision detection
- Speed adjustment
- Score calculation

### Object-Oriented Programming (OOP)
- Classes and objects used for:
  - Blocks
  - Game logic
  - GUI components
- Modular design for clean, readable, and maintainable code

---

## GUI & Visuals
- Built using **Java Swing**
- Provides:
  - Game window
  - Block animation
  - Score display
  - Game over screen

---

## Technologies Used
- **Java**
- **Java Swing**
- **Object-Oriented Programming (OOP)**
- **Stack Data Structure**

---

## Learning Outcomes
This project helped us:
- Apply **theoretical DSA concepts** to a real application
- Understand how **stacks are used in game mechanics**
- Improve skills in **GUI programming**
- Learn game state and event handling in Java

---

##  Screenshots
![Home](outputImages/Home.png)
![Gameplay](outputImagesoutputimages/gamePanel.png)
![Game Over](outputImages/gameOver.png)


---

## How to Run
1. Clone the repository  
   ```bash
   git clone https://github.com/Zahraoi/skyscraper-stack-builder.git
2. Open the project in IntelliJ / Eclipse / NetBeans
3. Run the main Java file
4. Enjoy the game 