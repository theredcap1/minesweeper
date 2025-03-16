package mines;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Board extends JPanel {
    private static final long serialVersionUID = 6195235521361212179L;

    private static final int NUM_IMAGES = 13;
    private static final int CELL_SIZE = 15;

    private static final int COVER_FOR_CELL = 10;
    private static final int MARK_FOR_CELL = 10;
    private static final int EMPTY_CELL = 0;
    private static final int MINE_CELL = 9;
    private static final int COVERED_MINE_CELL = 19;
    private static final int MARKED_MINE_CELL = 29;

    private static final int DRAW_MINE = 9;
    private static final int DRAW_COVER = 10;
    private static final int DRAW_MARK = 11;
    private static final int DRAW_WRONG_MARK = 12;

    private int[] field;
    private boolean inGame;
    private int minesLeft;
    private transient Image[] img;
    private static final int MINES = 40;
    private static final int ROWS = 16;
    private static final int COLS = 16;
    private static final int ALL_CELLS = 256;
    private JLabel statusbar;

    private SecureRandom random = new SecureRandom();

    public Board(JLabel statusbar) {
        this.statusbar = statusbar;

        img = new Image[NUM_IMAGES];
        for (int i = 0; i < NUM_IMAGES; i++) {
            img[i] = new ImageIcon(new File("bin/" + i + ".gif").getAbsolutePath()).getImage();
        }

        setDoubleBuffered(true);
        addMouseListener(new MinesAdapter());
        newGame();

    }

    public void newGame() {
        inGame = true;
        minesLeft = MINES;
        field = new int[ALL_CELLS];
        Arrays.fill(field, COVER_FOR_CELL);
        statusbar.setText(Integer.toString(minesLeft));

        placeMines();
    }

    private void placeMines() {
        int placedMines = 0;

        while (placedMines < MINES) {
            int position = (int) (ALL_CELLS * this.random.nextDouble());

            if (field[position] == COVERED_MINE_CELL) {
                continue;
            }

            field[position] = COVERED_MINE_CELL;
            placedMines++;
            updateAdjacentCells(position);
        }
    }

    private void updateAdjacentCells(int position) {
        int currentCol = position % COLS;
        int[] neighbors = {
                -COLS - 1, -COLS, -COLS + 1,
                -1, 1,
                COLS - 1, COLS, COLS + 1
        };

        for (int offset : neighbors) {
            int cell = position + offset;
            if (isValidCell(cell, currentCol, offset) && field[cell] != COVERED_MINE_CELL) {
                field[cell]++;
            }
        }
    }

    private boolean isValidCell(int cell, int currentCol, int offset) {
        if (cell < 0 || cell >= ALL_CELLS) {
            return false;
        }

        boolean isLeftEdge = (currentCol == 0);
        boolean isRightEdge = (currentCol == COLS - 1);

        return !((isLeftEdge && (offset == -COLS - 1 || offset == -1 || offset == COLS - 1)) ||
                (isRightEdge && (offset == -COLS + 1 || offset == 1 || offset == COLS + 1)));
    }

    @Override
    public void paint(Graphics g) {
        int uncover = 0;

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                int index = (i * COLS) + j;
                int cell = field[index];

                if (inGame && cell == MINE_CELL) {
                    inGame = false;
                }

                cell = determineCellState(cell);
                if (cell == DRAW_COVER) {
                    uncover++;
                }

                g.drawImage(img[cell], (j * CELL_SIZE), (i * CELL_SIZE), this);
            }
        }

        updateGameStatus(uncover);
    }

    private int determineCellState(int cell) {
        if (!inGame) {
            if (cell == COVERED_MINE_CELL)
                return DRAW_MINE;
            if (cell == MARKED_MINE_CELL)
                return DRAW_MARK;
            if (cell > COVERED_MINE_CELL)
                return DRAW_WRONG_MARK;
            if (cell > MINE_CELL)
                return DRAW_COVER;
        } else {
            if (cell > COVERED_MINE_CELL)
                return DRAW_MARK;
            if (cell > MINE_CELL)
                return DRAW_COVER;
        }
        return cell;
    }

    private void updateGameStatus(int uncover) {
        if (uncover == 0 && inGame) {
            inGame = false;
            statusbar.setText("Game won");
        } else if (!inGame) {
            statusbar.setText("Game lost");
        }
    }

    class MinesAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            int cCol = x / CELL_SIZE;
            int cRow = y / CELL_SIZE;
            int index = (cRow * COLS) + cCol;

            if (!inGame) {
                newGame();
                repaint();
                return;
            }

            if (x >= COLS * CELL_SIZE || y >= ROWS * CELL_SIZE)
                return;

            boolean rep = false;

            if (e.getButton() == MouseEvent.BUTTON3) {
                rep = handleRightClick(index);
            } else {
                rep = handleLeftClick(index);
            }

            if (rep)
                repaint();
        }

        private boolean handleRightClick(int index) {
            if (field[index] <= MINE_CELL)
                return false;

            if (field[index] <= COVERED_MINE_CELL) {
                if (minesLeft > 0) {
                    field[index] += MARK_FOR_CELL;
                    minesLeft--;
                    statusbar.setText(Integer.toString(minesLeft));
                } else {
                    statusbar.setText("No marks left");
                }
            } else {
                field[index] -= MARK_FOR_CELL;
                minesLeft++;
                statusbar.setText(Integer.toString(minesLeft));
            }

            return true;
        }

        private boolean handleLeftClick(int index) {
            if (field[index] > COVERED_MINE_CELL)
                return false;

            if (field[index] > MINE_CELL && field[index] < MARKED_MINE_CELL) {
                field[index] -= COVER_FOR_CELL;

                if (field[index] == MINE_CELL) {
                    inGame = false;
                } else if (field[index] == EMPTY_CELL) {
                    findEmptyCells(index);
                }

                return true;
            }

            return false;
        }

        private void findEmptyCells(int index) {
            int currentCol = index % COLS;
            int[] neighbors = {
                    -COLS - 1, -COLS, -COLS + 1,
                    -1, 1,
                    COLS - 1, COLS, COLS + 1
            };

            for (int offset : neighbors) {
                int cell = index + offset;

                if (isValidCell(cell, currentCol, offset) && field[cell] > MINE_CELL) {
                    field[cell] -= COVER_FOR_CELL;

                    // If the cell is empty, continue revealing its neighbors
                    if (field[cell] == EMPTY_CELL) {
                        findEmptyCells(cell);
                    }
                }
            }
        }
    }
}
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.event.MouseEvent;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MinesAdapterTest {

    @Mock
    private MouseEvent mouseEvent;

    @Mock
    private Board board;

    private MinesAdapter minesAdapter;

    @Before
    public void setup() {
        minesAdapter = new MinesAdapter(board);
    }

    @Test
    public void testMousePressed_GameNotInProgress_NewGameStarted() {
        // Arrange
        when(board.inGame).thenReturn(false);

        // Act
        minesAdapter.mousePressed(mouseEvent);

        // Assert
        verify(board).newGame();
        verify(board).repaint();
    }

    @Test
    public void testMousePressed_ClickOutsideGameArea_NoActionTaken() {
        // Arrange
        when(board.inGame).thenReturn(true);
        when(mouseEvent.getX()).thenReturn(Board.COLS * Board.CELL_SIZE + 1);
        when(mouseEvent.getY()).thenReturn(Board.ROWS * Board.CELL_SIZE + 1);

        // Act
        minesAdapter.mousePressed(mouseEvent);

        // Assert
        verify(board, never()).newGame();
        verify(board, never()).repaint();
    }

    @Test
    public void testMousePressed_RightClick_CellMarked() {
        // Arrange
        when(board.inGame).thenReturn(true);
        when(mouseEvent.getButton()).thenReturn(MouseEvent.BUTTON3);
        int index = 10;
        when(mouseEvent.getX()).thenReturn(index % Board.COLS * Board.CELL_SIZE);
        when(mouseEvent.getY()).thenReturn(index / Board.COLS * Board.CELL_SIZE);

        // Act
        minesAdapter.mousePressed(mouseEvent);

        // Assert
        verify(board).handleRightClick(index);
        verify(board).repaint();
    }

    @Test
    public void testMousePressed_LeftClick_CellRevealed() {
        // Arrange
        when(board.inGame).thenReturn(true);
        when(mouseEvent.getButton()).thenReturn(MouseEvent.BUTTON1);
        int index = 10;
        when(mouseEvent.getX()).thenReturn(index % Board.COLS * Board.CELL_SIZE);
        when(mouseEvent.getY()).thenReturn(index / Board.COLS * Board.CELL_SIZE);

        // Act
        minesAdapter.mousePressed(mouseEvent);

        // Assert
        verify(board).handleLeftClick(index);
        verify(board).repaint();
    }
}
