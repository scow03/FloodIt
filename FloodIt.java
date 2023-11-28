package FloodIt;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javalib.impworld.*;
import javalib.worldimages.*;
import tester.Tester;

// Represents a single square of the game area
class Cell {
  // In logical coordinates, with the origin at the top-left corner of the screen.
  int x;
  int y;
  Color color;
  boolean flooded;
  // the four adjacent cells to this one
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;

  Cell(int x, int y, Color color) {
    this.x = x;
    this.y = y;
    this.color = color;
    this.flooded = false;
    this.left = null;
    this.top = null;
    this.right = null;
    this.bottom = null;
  }

  // Produces WorldImage of Cell.
  public WorldImage draw() {
    return new RectangleImage(
        FloodItWorld.CELL_SIZE,
        FloodItWorld.CELL_SIZE,
        OutlineMode.SOLID,
        this.color);
  }

  // Returns true if this cell's color is the same as the provided floodColor.
  public boolean sameColor(Color floodColor) {
    return this.color.equals(floodColor);
  }

  // Floods this cell
  public static void flood(
      Cell c,
      Color floodColor,
      ArrayList<Cell> floodedCells,
      ArrayList<Cell> cellsToBeFlooded) {
    if (c != null && c.sameColor(floodColor)) {
      if (!c.flooded) {
        floodedCells.add(c);
        c.flooded = true;
      }
    }
  }

  // Adds new neighboring cells to list of all cells that have been flooded.
  public void floodMatchingNeighbors(
      Color floodColor,
      ArrayList<Cell> floodedCells,
      ArrayList<Cell> cellsToBeFlooded) {
    // Checks if neighboring cell matches flood color and
    // adds new flooded cells to list of flooded cells and
    // adds neighboring cells to list of cells to be flooded.
    Cell.flood(this.left, floodColor, floodedCells, cellsToBeFlooded);
    Cell.flood(this.top, floodColor, floodedCells, cellsToBeFlooded);
    Cell.flood(this.right, floodColor, floodedCells, cellsToBeFlooded);
    Cell.flood(this.bottom, floodColor, floodedCells, cellsToBeFlooded);
  }
}

class FloodItWorld extends World {
  // Static constants
  static double TICK_RATE = 0.0001;
  static int SECONDS_MOD = (int) ((1.0 / 28.0) / FloodItWorld.TICK_RATE);
  static int CELL_SIZE = 25;
  static int BOTTOM_PADDING = 100;
  static ArrayList<Color> COLORS = new ArrayList<Color>(Arrays.asList(
      Color.BLUE, Color.RED, Color.PINK, Color.GREEN,
      Color.GRAY, Color.YELLOW, Color.MAGENTA, Color.ORANGE));

  // Game board
  ArrayList<ArrayList<Cell>> board;

  // Instance fields
  WorldScene scene;
  Random rand;
  int boardSize;
  int screenWidth;
  int screenHeight;
  int numColors;
  int remainingTries;
  int currentTries;
  Color floodColor;
  ArrayList<Cell> floodedCells;
  ArrayList<Cell> cellsToBeFlooded;

  // Timer variables
  int seconds;
  int minutes;
  int hours;
  int currentTick;

  // Initial State
  FloodItWorld(int boardSize, int numColors) {
    this(boardSize, numColors, new Random());
  }

  // For random testing.
  FloodItWorld(int boardSize, int numColors, Random rand) {
    // Check for exception
    this.checkValidBoardSize(boardSize);
    this.checkValidNumColors(numColors);

    this.scene = getEmptyScene();
    this.rand = rand;
    this.boardSize = boardSize;
    this.numColors = numColors;
    this.screenWidth = (this.boardSize * FloodItWorld.CELL_SIZE);
    this.screenHeight = (this.boardSize * FloodItWorld.CELL_SIZE) + FloodItWorld.BOTTOM_PADDING;
    this.reset();
  }

  // Throws exception if board size is invalid.
  public void checkValidBoardSize(int boardSize) {
    if (boardSize < 2) {
      throw new IllegalArgumentException("Board size must be at least 2.");
    }
  }

  // Throws exception if number of colors is invalid.
  public void checkValidNumColors(int numColors) {
    if (numColors < 2 || numColors > FloodItWorld.COLORS.size()) {
      throw new IllegalArgumentException(
          "Number of colors must be between 2 and " + FloodItWorld.COLORS.size() + " inclusive.");
    }
  }

  // Resets the game;
  public void reset() {
    this.cellsToBeFlooded = new ArrayList<Cell>();
    this.initializeBoard();
    this.linkCells();
    this.remainingTries = boardSize - numColors <= 0 ? numColors : boardSize + (3 * numColors);
    this.currentTries = 0;
    this.seconds = 0;
    this.minutes = 0;
    this.hours = 0;
    this.currentTick = 0;
  }

  // Returns a random color.
  public Color getRandomColor() {
    int randIdx = this.rand.nextInt(this.numColors);
    return FloodItWorld.COLORS.get(randIdx);
  }

  // Properly links the left, top, right, bottom cell of each cell in the board.
  public void linkCells() {
    for (int i = 0; i < this.boardSize; i++) {
      for (int j = 0; j < this.boardSize; j++) {
        Cell currCell = this.board.get(i).get(j);

        // Set left
        if (j > 0) {
          Cell leftCell = this.board.get(i).get(j - 1);
          currCell.left = leftCell;
        }

        // Set top
        if (i > 0) {
          Cell topCell = this.board.get(i - 1).get(j);
          currCell.top = topCell;
        }

        // Set right
        if (j < this.boardSize - 1) {
          Cell rightCell = this.board.get(i).get(j + 1);
          currCell.right = rightCell;
        }

        // Set bottom
        if (i < this.boardSize - 1) {
          Cell bottomCell = this.board.get(i + 1).get(j);
          currCell.bottom = bottomCell;
        }
      }
    }
    Cell origin = this.board.get(0).get(0);
    origin.flooded = true;
    this.floodColor = origin.color;
    this.floodedCells = new ArrayList<Cell>(Arrays.asList(origin));
    this.assignFlood();
  }

  // Creates 2D arraylist of the starting game board.
  public void initializeBoard() {
    this.board = new ArrayList<ArrayList<Cell>>();

    for (int i = 0; i < this.boardSize; i++) {
      ArrayList<Cell> row = new ArrayList<Cell>();
      for (int j = 0; j < this.boardSize; j++) {
        int x = (j * FloodItWorld.CELL_SIZE) + (FloodItWorld.CELL_SIZE / 2);
        int y = (i * FloodItWorld.CELL_SIZE) + (FloodItWorld.CELL_SIZE / 2);
        row.add(new Cell(x, y, this.getRandomColor()));
      }
      this.board.add(row);
    }
  }

  // Returns true if mouseClick position is within board.
  public boolean isOnBoard(int mouseX, int mouseY) {
    Cell topLeftCell = this.board.get(0).get(0);
    Cell topRightCell = this.board.get(0).get(this.boardSize - 1);
    Cell bottomLeftCell = this.board.get(this.boardSize - 1).get(0);
    int topLeftX = topLeftCell.x - (FloodItWorld.CELL_SIZE / 2);
    int topLeftY = topLeftCell.y - (FloodItWorld.CELL_SIZE / 2);
    int topRightX = topRightCell.x + (FloodItWorld.CELL_SIZE / 2);
    int bottomLeftY = bottomLeftCell.y + (FloodItWorld.CELL_SIZE / 2);

    return (mouseX >= topLeftX && mouseX <= topRightX)
        && (mouseY >= topLeftY && mouseY <= bottomLeftY);
  }

  // Checks if player has flooded the entire board.
  public boolean didWin() {
    for (ArrayList<Cell> row : this.board) {
      for (Cell c : row) {
        if (!c.sameColor(this.floodColor)) {
          return false;
        }
      }
    }

    return true;
  }

  // Returns cell color on board that matches coordinates.
  public Color getSelectedColor(int mouseX, int mouseY) {
    int row = mouseY / FloodItWorld.CELL_SIZE;
    int col = mouseX / FloodItWorld.CELL_SIZE;
    Cell selectedCell = this.board.get(row).get(col);
    return selectedCell.color;
  }

  // Adds cells that need to be flooded to the list.
  public void assignFlood() {
    for (int i = 0; i < this.floodedCells.size(); i++) {
      Cell c = this.floodedCells.get(i);
      // Only add cells to be flooded if cell's color does not match floodColor.
      if (!c.sameColor(this.floodColor)) {
        this.cellsToBeFlooded.add(c);
      }
      c.floodMatchingNeighbors(
          this.floodColor,
          this.floodedCells,
          this.cellsToBeFlooded);
    }
  }

  // Handles flooding event onTick.
  public void handleFlooding() {
    // Handles flooding
    if (!this.cellsToBeFlooded.isEmpty()) {
      Cell c = this.cellsToBeFlooded.remove(0);
      c.color = this.floodColor;
    }
  }

  // Handles timer onTick.
  public void handleTimer() {
    // Handles timer
    if (this.currentTick % FloodItWorld.SECONDS_MOD == 0) {
      this.seconds++;
    }
    if (this.seconds == 60) {
      this.minutes++;
      this.seconds = 0;
    }
    if (this.minutes == 60) {
      this.hours++;
      this.minutes = 0;
    }
    this.currentTick++;
  }

  // Makes end scene with specified text in middle of screen.
  public WorldScene makeEndScene(String text) {
    WorldScene endScene = getEmptyScene();
    TextImage endText = new TextImage(text, 28, Color.BLACK);

    endScene.placeImageXY(endText, this.screenWidth / 2,
        this.screenHeight / 2);

    return endScene;
  }

  // Renders game board onto scene.
  public void renderBoard() {
    for (ArrayList<Cell> row : this.board) {
      for (Cell c : row) {
        this.scene.placeImageXY(c.draw(), c.x, c.y);
      }
    }
  }

  // Renders score onto scene.
  public void renderScore() {
    String scoreText = this.currentTries + "/" + this.remainingTries;
    WorldImage scoreImage = new TextImage(scoreText, 28, Color.BLACK);
    this.scene.placeImageXY(scoreImage, this.screenWidth / 2,
        this.screenHeight - (FloodItWorld.BOTTOM_PADDING / 2));
  }

  // Renders timer onto scene.
  public void renderTimer() {
    String seconds = String.format("%02d", this.seconds);
    String minutes = String.format("%02d", this.minutes);
    String hours = String.format("%02d", this.hours);
    String timer = hours + ":" + minutes + ":" + seconds;
    WorldImage timerImage = new TextImage(timer, 24, Color.BLUE);
    this.scene.placeImageXY(timerImage, this.screenWidth / 2,
        this.screenHeight - (FloodItWorld.BOTTOM_PADDING / 4));
  }

  // Handles on click.
  @Override
  public void onMouseClicked(Posn mouse) {
    // DONT DO ANYTHING IF:
    // - Cells are not finished flooding.
    // - User clicked outside of board.
    // - User clicked on color same as floodColor.
    if (this.cellsToBeFlooded.isEmpty() && this.isOnBoard(mouse.x, mouse.y)) {
      Color selectedColor = getSelectedColor(mouse.x, mouse.y);
      if (!selectedColor.equals(this.floodColor)) {
        this.floodColor = selectedColor;
        this.assignFlood();
        this.currentTries++;
      }
    }
  }

  // Handes key events.
  @Override
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      this.reset();
    }
  }

  // Handles end world.
  @Override
  public WorldEnd worldEnds() {
    boolean end = true;
    String message = "";

    if (this.currentTries == this.remainingTries
        && this.cellsToBeFlooded.isEmpty()) {
      message = "You Lost!";
    } else if (this.didWin()) {
      message = "You Won in "
          + String.format("%02d", this.hours) + ":"
          + String.format("%02d", this.minutes) + ":"
          + String.format("%02d", this.seconds) + " with "
          + this.currentTries + "/"
          + this.remainingTries + " tries!";
    } else {
      end = false;
    }
    return new WorldEnd(end, this.makeEndScene(message));
  }

  // Handles on tick.
  @Override
  public void onTick() {
    this.handleFlooding();
    this.handleTimer();
  }

  // Generates scene based on fields.
  @Override
  public WorldScene makeScene() {
    this.scene = getEmptyScene();
    this.renderBoard();
    this.renderScore();
    this.renderTimer();
    return this.scene;
  }
}

class ExamplesFloodIt {
  FloodItWorld game;

  // Tests big bang function
  void testBigBang(Tester t) {
    this.initGame();
    this.game.bigBang(
        this.game.screenWidth,
        this.game.screenHeight,
        FloodItWorld.TICK_RATE);
  }

  // Initializes conditions.
  void initGame() {
    this.game = new FloodItWorld(22, 4);
  }

  // Initializes conditions.
  void initCustom() {
    this.game = new FloodItWorld(2, 2, new Random(100));
  }

  // Tests draw method for Cell.
  void testDraw(Tester t) {
    this.initGame();

    Cell c1 = new Cell(0, 0, Color.BLUE);
    Cell c2 = new Cell(1, 1, Color.RED);
    Cell c3 = new Cell(0, 0, Color.GREEN);

    t.checkExpect(c1.draw(), new RectangleImage(
        FloodItWorld.CELL_SIZE,
        FloodItWorld.CELL_SIZE,
        OutlineMode.SOLID,
        Color.BLUE));

    t.checkExpect(c2.draw(), new RectangleImage(
        FloodItWorld.CELL_SIZE,
        FloodItWorld.CELL_SIZE,
        OutlineMode.SOLID,
        Color.RED));

    t.checkExpect(c3.draw(), new RectangleImage(
        FloodItWorld.CELL_SIZE,
        FloodItWorld.CELL_SIZE,
        OutlineMode.SOLID,
        Color.GREEN));
  }

  // Tests sameColor method for Cell.
  void testSameColor(Tester t) {
    Cell c = new Cell(0, 0, Color.BLUE);

    t.checkExpect(c.sameColor(Color.BLUE), true);
    t.checkExpect(c.sameColor(Color.RED), false);
  }

  // Tests flood method for Cell.
  void testFlood(Tester t) {
    this.initCustom();

    t.checkExpect(this.game.floodedCells.size(), 3);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);

    Cell c4 = this.game.board.get(1).get(1);
    Cell.flood(c4, this.game.floodColor, this.game.floodedCells,
        this.game.cellsToBeFlooded);

    t.checkExpect(this.game.floodedCells.size(), 3);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);
  }

  // Tests floodMatchingNeighbors method for Cell.
  void testFloodMatchingNeighbors(Tester t) {
    this.initCustom();

    t.checkExpect(this.game.floodedCells.size(), 3);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);

    Cell origin = this.game.board.get(0).get(0);
    origin.floodMatchingNeighbors(this.game.floodColor, this.game.floodedCells,
        this.game.cellsToBeFlooded);

    t.checkExpect(this.game.floodedCells.size(), 3);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);
  }

  // Tests checkValidBoardSize method for FloodItWorld.
  void testCheckValidBoardSize(Tester t) {
    this.initCustom();

    IllegalArgumentException exc = new IllegalArgumentException("Board size must be at least 2.");

    t.checkNoException(this.game, "checkValidBoardSize", 2);
    t.checkNoException(this.game, "checkValidBoardSize", 20);
    t.checkException(exc, this.game, "checkValidBoardSize", 1);
    t.checkException(exc, this.game, "checkValidBoardSize", 0);
    t.checkException(exc, this.game, "checkValidBoardSize", -1);
  }

  // Tests checkValidNumColors method for FloodItWorld.
  void testCheckValidNumColors(Tester t) {
    this.initCustom();

    IllegalArgumentException exc = new IllegalArgumentException(
        "Number of colors must be between 2 and " + FloodItWorld.COLORS.size() + " inclusive.");

    t.checkNoException(this.game, "checkValidNumColors", 2);
    t.checkNoException(this.game, "checkValidNumColors", 8);
    t.checkException(exc, this.game, "checkValidNumColors", 1);
    t.checkException(exc, this.game, "checkValidNumColors", -1);
    t.checkException(exc, this.game, "checkValidNumColors", 20);
  }

  // Tests reset method for FloodItWorld.
  void testReset(Tester t) {
    this.initCustom();

    this.game.reset();

    t.checkExpect(this.game.floodedCells.size(), 2);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);
    t.checkExpect(this.game.remainingTries,
        this.game.boardSize - this.game.numColors <= 0
            ? this.game.numColors
            : this.game.boardSize + (3 * this.game.numColors));
    t.checkExpect(this.game.currentTries, 0);
    t.checkExpect(this.game.seconds, 0);
    t.checkExpect(this.game.minutes, 0);
    t.checkExpect(this.game.hours, 0);
    t.checkExpect(this.game.currentTick, 0);
  }

  // Tests getRandomColor method for FloodItWorld.
  void testGetRandomColor(Tester t) {
    this.initCustom();

    FloodItWorld game2 = new FloodItWorld(10, 8, new Random(200));

    t.checkExpect(this.game.getRandomColor(), Color.RED);
    t.checkExpect(game2.getRandomColor(), Color.BLUE);
  }

  // Tests initializeBoard method for FloodItWorld.
  void testInitializeBoard(Tester t) {
    this.initCustom();

    Cell origin = this.game.board.get(0).get(0);

    t.checkExpect(this.game.board.size(), this.game.boardSize);
    t.checkExpect(this.game.board.get(0).size(), this.game.boardSize);
    t.checkExpect(origin.flooded, true);
    t.checkExpect(this.game.floodColor, origin.color);
  }

  // Tests linkCells method for FloodItWorld.
  void testLinkCells(Tester t) {
    this.initGame();

    Cell topLeft = this.game.board.get(0).get(0);
    Cell topRight = this.game.board.get(0).get(this.game.boardSize - 1);
    Cell bottomLeft = this.game.board.get(this.game.boardSize - 1).get(0);
    Cell bottomRight = this.game.board.get(this.game.boardSize - 1)
        .get(this.game.boardSize - 1);
    Cell center = this.game.board.get(this.game.boardSize / 2)
        .get(this.game.boardSize / 2);

    t.checkExpect(topLeft.top, null);
    t.checkExpect(topLeft.left, null);
    t.checkExpect(topLeft.right, this.game.board.get(0).get(1));
    t.checkExpect(topLeft.bottom, this.game.board.get(1).get(0));

    t.checkExpect(topRight.top, null);
    t.checkExpect(topRight.left, this.game.board.get(0)
        .get(this.game.boardSize - 2));
    t.checkExpect(topRight.right, null);
    t.checkExpect(topRight.bottom, this.game.board.get(1)
        .get(this.game.boardSize - 1));

    t.checkExpect(bottomLeft.top, this.game.board.get(this.game.boardSize - 2).get(0));
    t.checkExpect(bottomLeft.left, null);
    t.checkExpect(bottomLeft.right, this.game.board.get(this.game.boardSize - 1).get(1));
    t.checkExpect(bottomLeft.bottom, null);

    t.checkExpect(bottomRight.top, this.game.board.get(this.game.boardSize - 2)
        .get(this.game.boardSize - 1));
    t.checkExpect(bottomRight.left, this.game.board.get(this.game.boardSize - 1)
        .get(this.game.boardSize - 2));
    t.checkExpect(bottomRight.right, null);
    t.checkExpect(bottomRight.bottom, null);

    t.checkExpect(center.top, this.game.board.get(this.game.boardSize / 2 - 1)
        .get(this.game.boardSize / 2));
    t.checkExpect(center.left, this.game.board.get(this.game.boardSize / 2)
        .get(this.game.boardSize / 2 - 1));
    t.checkExpect(center.right, this.game.board.get(this.game.boardSize / 2)
        .get(this.game.boardSize / 2 + 1));
    t.checkExpect(center.bottom, this.game.board.get(this.game.boardSize / 2 + 1)
        .get(this.game.boardSize / 2));

    Cell origin = this.game.board.get(0).get(0);
    t.checkExpect(this.game.floodedCells, new ArrayList<Cell>(Arrays.asList(origin)));
  }

  // Tests isOnBoard method for FloodItWorld.
  void testIsOnBoard(Tester t) {
    this.initCustom();

    t.checkExpect(this.game.isOnBoard(0, 0), true);
    t.checkExpect(this.game.isOnBoard(49, 49), true);
    t.checkExpect(this.game.isOnBoard(49, 0), true);
    t.checkExpect(this.game.isOnBoard(0, 49), true);
    t.checkExpect(this.game.isOnBoard(100, 100), false);
  }

  // Tests didWin method for FloodItWorld.
  void testDidWin(Tester t) {
    this.initCustom();

    t.checkExpect(this.game.didWin(), false);

    for (ArrayList<Cell> row : this.game.board) {
      for (Cell c : row) {
        c.flooded = true;
        c.color = this.game.floodColor;
      }
    }

    t.checkExpect(this.game.didWin(), true);
  }

  // Tests getSelectedColor method for FloodItWorld.
  void testGetSelectedColor(Tester t) {
    this.initCustom();

    Cell origin = this.game.board.get(0).get(0);
    t.checkExpect(this.game.getSelectedColor(0, 0), origin.color);
    t.checkExpect(this.game.getSelectedColor(26, 0), origin.right.color);
  }

  // Tests assignFlood method for FloodItWorld.
  void testAssignFlood(Tester t) {
    this.initCustom();

    t.checkExpect(this.game.floodedCells.size(), 3);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);

    this.game.assignFlood();

    t.checkExpect(this.game.floodedCells.size(), 3);
  }

  // Tests handleFlooding method for FloodItWorld.
  void testHandleFlooding(Tester t) {
    this.initCustom();

    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);
    t.checkExpect(this.game.floodedCells.size(), 3);

    this.game.assignFlood();

    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);
    t.checkExpect(this.game.floodedCells.size(), 3);
  }

  // Tests handleTimer method for FloodItWorld.
  void testHandleTimer(Tester t) {
    this.initCustom();

    t.checkExpect(this.game.seconds, 0);
    t.checkExpect(this.game.minutes, 0);
    t.checkExpect(this.game.hours, 0);

    this.game.currentTick = FloodItWorld.SECONDS_MOD;

    this.game.handleTimer();

    t.checkExpect(this.game.seconds, 1);
    t.checkExpect(this.game.minutes, 0);
    t.checkExpect(this.game.hours, 0);

    this.game.seconds = 59;
    this.game.currentTick = FloodItWorld.SECONDS_MOD;

    this.game.handleTimer();

    t.checkExpect(this.game.seconds, 0);
    t.checkExpect(this.game.minutes, 1);
    t.checkExpect(this.game.hours, 0);

    this.game.seconds = 59;
    this.game.minutes = 59;

    this.game.currentTick = FloodItWorld.SECONDS_MOD;

    this.game.handleTimer();

    t.checkExpect(this.game.seconds, 0);
    t.checkExpect(this.game.minutes, 0);
    t.checkExpect(this.game.hours, 1);
  }

  // Tests makeEndScene method for FloodItWorld.
  void testMakeEndScene(Tester t) {
    this.initCustom();

    WorldScene s1 = this.game.getEmptyScene();
    TextImage t1 = new TextImage("Hello", 28, Color.BLACK);
    s1.placeImageXY(t1, this.game.screenWidth / 2,
        this.game.screenHeight / 2);
    WorldScene s2 = this.game.getEmptyScene();
    TextImage t2 = new TextImage("Goodbye", 28, Color.BLACK);
    s2.placeImageXY(t2, this.game.screenWidth / 2,
        this.game.screenHeight / 2);

    t.checkExpect(this.game.makeEndScene("Hello"), s1);
    t.checkExpect(this.game.makeEndScene("Goodbye"), s2);
  }

  // Tests renderBoard method for FloodItWorld.
  void testRenderBoard(Tester t) {
    this.initCustom();

    WorldScene s1 = this.game.getEmptyScene();
    for (ArrayList<Cell> row : this.game.board) {
      for (Cell c : row) {
        s1.placeImageXY(c.draw(), c.x, c.y);
      }
    }

    t.checkExpect(this.game.scene, this.game.getEmptyScene());

    this.game.renderBoard();

    t.checkExpect(this.game.scene, s1);
  }

  // Tests renderScore method for FloodItWorld.
  void testRenderScore(Tester t) {
    this.initCustom();

    WorldScene s1 = this.game.getEmptyScene();
    String scoreText = this.game.currentTries + "/" + this.game.remainingTries;
    WorldImage scoreImage = new TextImage(scoreText, 28, Color.BLACK);
    s1.placeImageXY(scoreImage, this.game.screenWidth / 2,
        this.game.screenHeight - (FloodItWorld.BOTTOM_PADDING / 2));

    t.checkExpect(this.game.scene, this.game.getEmptyScene());

    this.game.renderBoard();

    t.checkExpect(this.game.scene, s1);
  }

  // Tests renderTimer method for FloodItWorld.
  void testRenderTimer(Tester t) {
    this.initCustom();

    WorldScene s1 = this.game.getEmptyScene();
    String seconds = String.format("%02d", this.game.seconds);
    String minutes = String.format("%02d", this.game.minutes);
    String hours = String.format("%02d", this.game.hours);
    String timer = hours + ":" + minutes + ":" + seconds;
    WorldImage timerImage = new TextImage(timer, 24, Color.BLUE);
    s1.placeImageXY(timerImage, this.game.screenWidth / 2,
        this.game.screenHeight - (FloodItWorld.BOTTOM_PADDING / 4));

    t.checkExpect(this.game.scene, this.game.getEmptyScene());

    this.game.renderTimer();

    t.checkExpect(this.game.scene, s1);
  }

  // Tests onMouseClicked method for FloodItWorld.
  void testOnMouseClicked(Tester t) {
    this.initCustom();

    // If user pressed outside board, nothing should change.
    this.game.onMouseClicked(new Posn(300, 300));
    t.checkExpect(this.game.currentTries, 0);

    // If user pressed same color cell.
    this.game.onMouseClicked(new Posn(1, 1));
    t.checkExpect(this.game.currentTries, 0);

    // If user pressed different color cell (should flood).
    this.game.onMouseClicked(new Posn(12, 37));
    t.checkExpect(this.game.currentTries, 1);
  }

  // Tests onKeyEvent method for FloodItWorld.
  void testOnKeyEvent(Tester t) {
    this.initCustom();

    this.game.onMouseClicked(new Posn(12, 37));

    t.checkExpect(this.game.currentTries, 1);

    this.game.onKeyEvent("r");

    t.checkExpect(this.game.currentTries, 0);
    t.checkExpect(this.game.floodedCells.size(), 2);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);
  }

  // Tests worldEnds method for FloodItWorld.
  void testWorldEnds(Tester t) {
    this.initCustom();

    WorldEnd notEnd = new WorldEnd(false, this.game.makeEndScene(""));
    WorldEnd endLost = new WorldEnd(true, this.game.makeEndScene("You Lost!"));
    WorldEnd endWin = new WorldEnd(true, this.game.makeEndScene(
        "You Won in "
            + String.format("%02d", this.game.hours) + ":"
            + String.format("%02d", this.game.minutes) + ":"
            + String.format("%02d", this.game.seconds) + " with "
            + this.game.currentTries + "/"
            + this.game.remainingTries + " tries!"));

    // Test world not ended yet.
    t.checkExpect(this.game.worldEnds(), notEnd);

    // Test world end on a lost.
    this.game.currentTries = this.game.remainingTries;
    t.checkExpect(this.game.didWin(), false);
    t.checkExpect(this.game.worldEnds(), endLost);

    // Test world end on a win.
    for (ArrayList<Cell> row : this.game.board) {
      for (Cell c : row) {
        c.flooded = true;
        c.color = this.game.floodColor;
      }
    }

    t.checkExpect(this.game.didWin(), true);
    t.checkExpect(this.game.worldEnds(), endWin);
  }

  // Tests onTick method for FloodItWorld.
  void testOnTick(Tester t) {
    this.initCustom();

    Cell origin = this.game.board.get(0).get(0);
    this.game.cellsToBeFlooded.add(origin);

    t.checkExpect(this.game.currentTick, 0);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 1);

    this.game.onTick();

    // Check if tick has incremented and
    // cell in cellsToBeFlooded was removed.
    t.checkExpect(this.game.currentTick, 1);
    t.checkExpect(this.game.cellsToBeFlooded.size(), 0);
  }

  // Tests makeScene method for FloodItWorld.
  void testMakeScene(Tester t) {
    this.initCustom();

    WorldScene scene = this.game.getEmptyScene();
    for (ArrayList<Cell> row : this.game.board) {
      for (Cell c : row) {
        scene.placeImageXY(c.draw(), c.x, c.y);
      }
    }

    t.checkExpect(this.game.makeScene(), scene);
  }
}
