# gdx-dungeon-game

A dungeon crawler game developed with [libGDX](https://libgdx.com/) and Kotlin.

## Features

- **Procedurally Generated Dungeons:** Every playthrough is unique, with a new dungeon layout generated each time.
- **Optimal Path Visualization:** The game calculates and displays the optimal path for the knight to traverse the dungeon.
- **Minimum Health Calculation:** It determines the minimum health required for the knight to survive the journey.
- **Dynamic UI:** The game features a simple UI with a button to generate new maps.

## Gameplay

The player controls a knight who must navigate a dungeon filled with demons and magic orbs. The objective is to find the safest path from the top-left corner to the bottom-right corner of the dungeon.

- **Demons:** Represented by negative numbers, they decrease the knight's health.
- **Magic Orbs:** Represented by positive numbers, they increase the knight's health.
- **Empty Cells:** Represented by zeros, they are safe to traverse.

The game calculates the minimum initial health the knight needs to start with to complete the dungeon without their health dropping to zero or below. The player can trigger an animation to watch the knight move along this optimal path.

## Build and Run

This project uses Gradle as a build tool.

### Prerequisites

- Java Development Kit (JDK) 17 or higher.

### Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/gdx-dungeon-game.git
   cd gdx-dungeon-game
   ```

2. **Build the project:**
   - On Windows, run:
     ```bash
     gradlew.bat build
     ```
   - On macOS and Linux, run:
     ```bash
     ./gradlew build
     ```

3. **Run the game:**
   - On Windows, run:
     ```bash
     gradlew.bat lwjgl3:run
     ```
   - On macOS and Linux, run:
     ```bash
     ./gradlew lwjgl3:run
     ```

## Packaging

To package the application, you can use the following Gradle tasks:

- **Create a runnable JAR:**
  ```bash
  ./gradlew lwjgl3:dist
  ```
  This will create a JAR file in the `lwjgl3/build/libs/` directory.

- **Create a platform-specific distribution with an embedded JRE:**
  - **Windows:**
    ```bash
    ./gradlew lwjgl3:construoWinX64
    ```
  - **Linux:**
    ```bash
    ./gradlew lwjgl3:construoLinuxX64
    ```
  - **macOS (Intel):**
    ```bash
    ./gradlew lwjgl3:construoMacX64
    ```
  - **macOS (Apple Silicon):**
    ```bash
    ./gradlew lwjgl3:construoMacM1
    ```
  These tasks will create a distribution in the `lwjgl3/build/construo/` directory.