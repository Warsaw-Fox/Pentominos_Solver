/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.pentomimo;

/**
 *
 * @author Kacper
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Ta klasa rozwiązuje łamigłówki pentomino. Pentomino składa się z 5 połączonych kwadratów. 
 * Istnieje dokładnie 12 sposobów na ułożenie pentomino (licząc rotacje i odbicia jako ten sam kawałek). 
 * Zagadka pentomino polega na próbie umieszczenia tych kawałków na planszy, bez nakładania się na siebie. 
 * Najczęstszą wersją jest plansza o rozmiarze 8 na 8. W tym przypadku pentominy wypełnią 60 z dostępnych 64 kwadratów na planszy. 
 * Kwadraty, które mają pozostać puste, zazwyczaj są wcześniej określone. 
 * Możliwe są także inne rozmiary plansz, w tym mniejsze plansze, które nie pomieszczą wszystkich kawałków, oraz niektóre rozmiary plansz, 
 * na których wszystkie kawałki zmieszczą się dokładnie, bez nadmiarowych miejsc.
 */
public class PentominosPanel extends JPanel {
   
   private MosaicPanel board;  // Wyświetlanie planszy na ekranie
   
   private JLabel comment;   // komentarz statusu wyświetlany pod planszą
   
   private boolean[] used = new boolean[13];  //  used[i] informuje, czy element o numerze i jest już na planszy
   
   private int numused;     // Liczba elementów obecnie na planszy, od 0 do 12.
   
   private GameThread gameThread = null;   // Wątek do uruchomienia procedury rozwiązywania łamigłówki.
   
   private JMenuItem restartAction,restartClearAction,restartRandomAction;  // Pozycje menu dla poleceń użytkownika.
    private JMenuItem goAction,pauseAction,stepAction,saveAction,quitAction; 
    private JMenuItem oneSidedAction;
    private JCheckBoxMenuItem randomizePiecesChoice, checkForBlocksChoice, symmetryCheckChoice;

   private JRadioButtonMenuItem[] speedChoice = new JRadioButtonMenuItem[7];  // Pozycje menu do ustawiania prędkości.
   
   private final int[]  speedDelay = { 5, 25, 100, 500, 1000 };  // Czasy opóźnień między ruchami dla prędkości 2-6.
   
   volatile private int selectedSpeed = 4;  // Początkowa domyślna prędkość i odpowiadające jej opóźnienie.
   volatile private int delay = 100;        
   
   private boolean creatingBoard;  // To jest prawdą, gdy użytkownik ustawia planszę.
   private int clickCt;  // Liczba kwadratów, które zostały zamalowane przez użytkownika - patrz w rutynie mousePressed.
   
   private final static int GO_MESSAGE = 1;      // Wartości dla zmiennej message.   
   private final static int STEP_MESSAGE = 2;
   private final static int PAUSE_MESSAGE = 3;
   private final static int RESTART_MESSAGE = 4;
   private final static int RESTART_CLEAR_MESSAGE = 5;
   private final static int RESTART_RANDOM_MESSAGE = 6;
   private final static int TERMINATE_MESSAGE = 7;
   
   
   private int rows, cols;  // Liczba wierszy i kolumn na planszy.
   
   private int piecesNeeded; // Ile elementów potrzeba, aby wypełnić planszę tak dużo, jak to możliwe. Zawsze <= 12.
   private int spareSpaces;  // Liczba dodatkowych pustych miejsc po umieszczeniu wymaganej liczby elementów.
   
   
   private MouseListener mouseHandler = new MouseAdapter() {
      /**
       * Procedura MousePressed obsługuje wybieranie pustych miejsc, które mają pozostać puste. 
       * Gdy wszystkie puste miejsca zostały już wybrane, rozpoczyna się proces znajdowania rozwiązania.
       */
      public void mousePressed(MouseEvent evt) {
         if (creatingBoard) {
            int col = board.xCoordToColumnNumber(evt.getX());
            int row = board.yCoordToRowNumber(evt.getY());
            if (col < 0 || col >= cols || row < 0 || row >= rows)
               return;
            if (board.getColor(row,col) == null && clickCt < spareSpaces) {
               board.setColor(row,col,emptyColor);
               clickCt++;
               if (clickCt == spareSpaces)
                  comment.setText("Use \"Go\" to Start (or click a black square)");
               else
                  comment.setText("Click (up to) " + (spareSpaces-clickCt) + " squares.");
            }
            else if (board.getColor(row,col) != null && clickCt > 0){
               board.setColor(row,col,null);
               clickCt--;
               comment.setText("Click (up to) " + (spareSpaces-clickCt) + " squares.");
            }
         }
      }

   };
   
   private ActionListener menuHandler = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
         Object source = evt.getSource();
         if (source == restartAction) {
            pauseAction.setEnabled(false);
            stepAction.setEnabled(false);
            gameThread.setMessage(RESTART_MESSAGE);
         }
         else if (source == restartClearAction) {
            pauseAction.setEnabled(false);
            stepAction.setEnabled(false);
            gameThread.setMessage(RESTART_CLEAR_MESSAGE);
         }
         else if (source == restartRandomAction) {
            pauseAction.setEnabled(false);
            stepAction.setEnabled(false);
            gameThread.setMessage(RESTART_RANDOM_MESSAGE);
         }
         else if (source == goAction) {
            pauseAction.setEnabled(true);
            stepAction.setEnabled(false);
            gameThread.setMessage(GO_MESSAGE);
         }
         else if (source == pauseAction) {
            pauseAction.setEnabled(false);
            stepAction.setEnabled(true);
            gameThread.setMessage(PAUSE_MESSAGE);
         }
         else if (source == stepAction) {
            gameThread.setMessage(STEP_MESSAGE);
         }
         else if (source == checkForBlocksChoice)
            gameThread.checkForBlocks = checkForBlocksChoice.isSelected();
         else if (source == randomizePiecesChoice)
            gameThread.randomizePieces = randomizePiecesChoice.isSelected();
         else if (source == symmetryCheckChoice)
            gameThread.symmetryCheck = symmetryCheckChoice.isSelected();
         else if (source == oneSidedAction)
            doOneSidedCommand();
         else if (source == saveAction)
            doSaveImage();
         else if (source == quitAction)
            System.exit(0);
         else if (source instanceof JRadioButtonMenuItem) {
            JRadioButtonMenuItem item = ((JRadioButtonMenuItem)source);
            int i;
            for (i = 0; i < speedChoice.length; i++) {
               if (speedChoice[i] == item)
                  break;
            }
            if (i == speedChoice.length || i == selectedSpeed)
               return;
            selectedSpeed = i;
            if (selectedSpeed < 2)
               delay = 0;
            else
               delay = speedDelay[selectedSpeed-2];
            if (gameThread.running)
               board.setAutopaint( selectedSpeed > 1 );
            board.repaint();
            gameThread.doDelay(25);
         }
      }
   };
   
   /**
    * Ta struktura danych reprezentuje klocki. Jest 12 klocków, a każdy z nich może być obracany i odwracany. 
    * Niektóre z tych ruchów zmieniają klocek ze względu na symetrię. Każda różna pozycja każdego klocka ma swój wiersz w tej tablicy. 
    * Każdy wiersz składa się z 9 elementów. Pierwszy element to numer klocka, od 1 do 12. 
    * Pozostałe 8 elementów opisuje kształt klocka w następujący nietypowy sposób: 
    * Założono, że jeden kwadrat znajduje się na pozycji (0,0) w siatce; ten kwadrat jest wybierany jako "narożny górny lewy" kwadrat w klocku, 
    * w sensie, że wszystkie inne kwadraty są albo po prawej stronie tego kwadratu w tym samym rzędzie, albo w niższych rzędach. 
    * Pozostałe 4 kwadraty w klocku są zakodowane za pomocą 8 liczb, które określają rzęd i kolumnę każdego z pozostałych kwadratów. 
    * Jeśli osiem liczb opisujących klocek to (a,b,c,d,e,f,g,h), 
    * to gdy klocek jest umieszczany na planszy z narożnym górnym lewym kwadratem na pozycji (r,c), 
    * pozostałe kwadraty znajdą się na pozycjach (r+a,c+b), (r+c,c+d), (r+e,c+f) i (r+g,c+h). 
    * Ta reprezentacja jest używana w metodach putPiece() i removePiece().
    */
   private  static final int[][] piece_data = {
      { 1, 0,1,0,2,0,3,0,4 },  // Opisuje klocek 1 (pentomino "I") w jego poziomej orientacji.
      { 1, 1,0,2,0,3,0,4,0 },  // Opisuje klocek 1 (pentomino "I") w jego pionowej orientacji.
      { 2, 1,-1,1,0,1,1,2,0 }, // Pentomino "X" w swojej jedynie orientacji.
      { 3, 0,1,1,0,2,-1,2,0 }, // itd.
      { 3, 1,0,1,1,1,2,2,2 },
      { 3, 0,1,1,1,2,1,2,2 },
      { 3, 1,-2,1,-1,1,0,2,-2 },
      { 4, 1,0,2,0,2,1,2,2 },
      { 4, 0,1,0,2,1,0,2,0 },
      { 4, 1,0,2,-2,2,-1,2,0 },
      { 4, 0,1,0,2,1,2,2,2 },
      { 5, 0,1,0,2,1,1,2,1 },
      { 5, 1,-2,1,-1,1,0,2,0 },
      { 5, 1,0,2,-1,2,0,2,1 },
      { 5, 1,0,1,1,1,2,2,0 },
      { 6, 1,0,1,1,2,1,2,2 },
      { 6, 1,-1,1,0,2,-2,2,-1 },
      { 6, 0,1,1,1,1,2,2,2 },
      { 6, 0,1,1,-1,1,0,2,-1 },
      { 7, 0,1,0,2,1,0,1,2 },
      { 7, 0,1,1,1,2,0,2,1 },
      { 7, 0,2,1,0,1,1,1,2 },
      { 7, 0,1,1,0,2,0,2,1 },
      { 8, 1,0,1,1,1,2,1,3 },
      { 8, 1,0,2,0,3,-1,3,0 },
      { 8, 0,1,0,2,0,3,1,3 },
      { 8, 0,1,1,0,2,0,3,0 },
      { 8, 0,1,1,1,2,1,3,1 },
      { 8, 0,1,0,2,0,3,1,0 },
      { 8, 1,0,2,0,3,0,3,1 },
      { 8, 1,-3,1,-2,1,-1,1,0 },
      { 9, 0,1,1,-2,1,-1,1,0 },
      { 9, 1,0,1,1,2,1,3,1 },
      { 9, 0,1,0,2,1,-1,1,0 },
      { 9, 1,0,2,0,2,1,3,1 },
      { 9, 0,1,1,1,1,2,1,3 },
      { 9, 1,0,2,-1,2,0,3,-1 },
      { 9, 0,1,0,2,1,2,1,3 },
      { 9, 1,-1,1,0,2,-1,3,-1 },
      { 10, 1,-2,1,-1,1,0,1,1 },
      { 10, 1,-1,1,0,2,0,3,0 },
      { 10, 0,1,0,2,0,3,1,1 },
      { 10, 1,0,2,0,2,1,3,0 },
      { 10, 0,1,0,2,0,3,1,2 },
      { 10, 1,0,1,1,2,0,3,0 },
      { 10, 1,-1,1,0,1,1,1,2 },
      { 10, 1,0,2,-1,2,0,3,0 },
      { 11, 1,-1,1,0,1,1,2,1 },
      { 11, 0,1,1,-1,1,0,2,0 },
      { 11, 1,0,1,1,1,2,2,1 },
      { 11, 1,0,1,1,2,-1,2,0 },
      { 11, 1,-2,1,-1,1,0,2,-1 },
      { 11, 0,1,1,1,1,2,2,1 },
      { 11, 1,-1,1,0,1,1,2,-1 },
      { 11, 1,-1,1,0,2,0,2,1 },
      { 12, 0,1,1,0,1,1,2,1 },
      { 12, 0,1,0,2,1,0,1,1 },
      { 12, 1,0,1,1,2,0,2,1 },
      { 12, 0,1,1,-1,1,0,1,1 },
      { 12, 0,1,1,0,1,1,1,2 },
      { 12, 1,-1,1,0,2,-1,2,0 },
      { 12, 0,1,0,2,1,1,1,2 },
      { 12, 0,1,1,0,1,1,2,0 }
   };
   
   private Color pieceColor[] = {  // Kolory kształtów numer 1 do 12; pieceColor[0] nie jest używany.
         null,
         new Color(200,0,0),
         new Color(150,150,255),
         new Color(0,200,200),
         new Color(255,150,255),
         new Color(0,200,0),
         new Color(150,255,255),
         new Color(200,200,0),
         new Color(0,0,200),
         new Color(255,150,150),
         new Color(200,0,200),
         new Color(255,255,150),
         new Color(150,255,150)
   };
   
   private final static Color emptyColor = Color.BLACK; // Kolor kwadratu, który użytkownik wybrał, aby pozostał pusty.
   
   private static final int SYMMETRY_NONE = -1;   // Potencjalne typy symetrii planszy, używane w GameThread.checkSymmetries.
   private static final int SYMMETRY_V = 0;
   private static final int SYMMETRY_H = 1;
   private static final int SYMMETRY_R180 = 2;
   private static final int SYMMETRY_HV = 3;   // R180
   private static final int SYMMETRY_D1 = 4;
   private static final int SYMMETRY_D2 = 5;
   private static final int SYMMETRY_D1D2 = 6;  // R180
   private static final int SYMMETRY_R90 = 7;   // R180, R270
   private static final int SYMMETRY_ALL = 8;
   
   private static final int[][] remove_for_symmetry = { // Usuwanie kawałków pozwala eliminować rozwiązania, które są tylko odbiciami lub obrótami innych rozwiązań.
      { 9,10 },  // Kawałki do usunięcia dla typu symetrii 0 = SYMMETRY_V, itd.
      { 8,10 }, 
      { 9,10 },
      { 8,9,10 },
      { 1 },
      { 1 },
      { 12, 13, 14 },
      { 8,9,10},
      { 1,8,9,10}
   };

   private final static int[][][] side_info = { // Pozycje kawałków dla obu stron dwustronnych pentominos; używane w implementacji polecenia "One Sided".
      { {27, 28, 29, 30}, {23, 24, 25, 26} }, // Strony A i B dla pentomino "L".
      { {35, 36, 37, 38}, {31, 32, 33, 34} }, // dla "N" 
      { {43, 44, 45, 46}, {39, 40, 41, 42} }, // dla "Y" 
      { {47, 48, 49, 50}, {51, 52, 53, 54} }, // dla "R" 
      { {59, 60, 61, 62}, {55, 56, 57, 58} }, // dla "P" 
      { {3, 4}, {5, 6} }                      // dla "Z" 
   };
   
      
   /**
    *Tworzy planszę pentominos o rozmiarze 8 wierszy na 8 kolumn.
    */
   public PentominosPanel() {
      this(8,8,true);
   }
   
   /**
    * Tworzy planszę pentominos o określonej liczbie wierszy i kolumn, które muszą wynosić co najmniej 3:
    * Jeśli opcja "autostart" jest ustawiona na "true", program tworzy losową planszę i rozpoczyna rozwiązywanie natychmiast.*
    */
   public PentominosPanel(int rowCt, int colCt, boolean autostart) {
      
      setLayout(new BorderLayout(5,5));
      setBackground(Color.LIGHT_GRAY);
      
      rows = rowCt;
      if (rows < 3)
         rows = 8;
      if (cols < 3)
         cols = 8;
      cols = colCt;

      Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
      int rowsize = (screensize.height - 100)/rows;
      if (rowsize > 35)
         rowsize = 35;  // Maksymalny rozmiar kwadratów.
      else if (rowsize < 4)
         rowsize = 4;
      int colsize = (screensize.width - 50)/rows;
      if (colsize > 35)
         colsize = 35;  // Maksymalny rozmiar kwadratów.
      else if (colsize < 4)
         colsize = 4;
      int size = Math.min(rowsize,colsize);
      board = new MosaicPanel(rowCt,colCt,size,size);  // do wyświetlania planszy
      board.setAlwaysDrawGrouting(true);
      board.setDefaultColor(Color.WHITE);
      board.setGroutingColor(Color.LIGHT_GRAY);
      add(board,BorderLayout.CENTER);
      
      comment = new JLabel("", JLabel.CENTER);
      comment.setFont(new Font("TimesRoman", Font.BOLD, 14));
      add(comment, BorderLayout.SOUTH);
      
      JPanel right = new JPanel();                // przechowuje przyciski kontrolne
      right.setLayout(new GridLayout(6,1,5,5));
      restartAction = new JMenuItem("Restart");
      restartClearAction = new JMenuItem("Restart / Empty Board");
      restartRandomAction = new JMenuItem("Restart / Random");
      goAction = new JMenuItem("Go");
      pauseAction = new JMenuItem("Pause");
      stepAction = new JMenuItem("Step");
      saveAction = new JMenuItem("Save Image...");
      quitAction = new JMenuItem("Quit");
      randomizePiecesChoice = new JCheckBoxMenuItem("Randomize Order of Pieces");
      checkForBlocksChoice = new JCheckBoxMenuItem("Check for Obvious Blocking");
      symmetryCheckChoice = new JCheckBoxMenuItem("Symmetry Check");
      oneSidedAction  = new JMenuItem("One Sided [Currently OFF]...");
      
      String commandKey;
      commandKey = "control ";
      try {
         String OS = System.getProperty("os.name");
         if (OS.startsWith("Mac"))
            commandKey = "meta ";
      }
      catch (Exception e) {
      }

      restartAction.addActionListener(menuHandler);
      restartClearAction.addActionListener(menuHandler);
      restartRandomAction.addActionListener(menuHandler);
      goAction.addActionListener(menuHandler);
      pauseAction.addActionListener(menuHandler);
      stepAction.addActionListener(menuHandler);
      saveAction.addActionListener(menuHandler);
      quitAction.addActionListener(menuHandler);
      randomizePiecesChoice.addActionListener(menuHandler);
      checkForBlocksChoice.addActionListener(menuHandler);
      symmetryCheckChoice.addActionListener(menuHandler);
      oneSidedAction.addActionListener(menuHandler);
      goAction.setAccelerator(KeyStroke.getKeyStroke(commandKey + "G"));
      pauseAction.setAccelerator(KeyStroke.getKeyStroke(commandKey + "P"));
      stepAction.setAccelerator(KeyStroke.getKeyStroke(commandKey + "S"));
      restartAction.setAccelerator(KeyStroke.getKeyStroke(commandKey + "R"));
      restartClearAction.setAccelerator(KeyStroke.getKeyStroke(commandKey + "E"));
      restartRandomAction.setAccelerator(KeyStroke.getKeyStroke(commandKey + "D"));
      quitAction.setAccelerator(KeyStroke.getKeyStroke(commandKey + "Q"));
      
      ButtonGroup group = new ButtonGroup();
      speedChoice[0] = new JRadioButtonMenuItem("Solutions Only / No Stop");
      speedChoice[1] = new JRadioButtonMenuItem("Very Fast (Limited Graphics)");
      speedChoice[2] = new JRadioButtonMenuItem("Faster");
      speedChoice[3] = new JRadioButtonMenuItem("Fast");
      speedChoice[4] = new JRadioButtonMenuItem("Moderate");
      speedChoice[5] = new JRadioButtonMenuItem("Slow");
      speedChoice[6] = new JRadioButtonMenuItem("Slower");
      for (int i = 0; i < 7; i++) {
         group.add(speedChoice[i]);
         speedChoice[i].addActionListener(menuHandler);
         speedChoice[i].setAccelerator(KeyStroke.getKeyStroke(commandKey + (char)('0' + i)));
      }
      speedChoice[4].setSelected(true);
      
      board.addMouseListener(mouseHandler);
      
      piecesNeeded = (rows*cols)/5;
      if (piecesNeeded > 12)
         piecesNeeded = 12;
      spareSpaces = rows*cols - 5*piecesNeeded;
      if (spareSpaces > 0)
         comment.setText("Click (up to) " + spareSpaces + " squares");
      creatingBoard = spareSpaces > 0;
      clickCt = 0;
      
      setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY,5));
      board.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY,2));
      
      gameThread = new GameThread();
      
      if (autostart) {
         gameThread.setMessage(RESTART_RANDOM_MESSAGE);
         pauseAction.setEnabled(true);
         stepAction.setEnabled(false);
         creatingBoard = false;
      }
      else {
         pauseAction.setEnabled(false);
         creatingBoard = spareSpaces > 0;
         if (creatingBoard)
            comment.setText("Select Squares or Use \"Go\" to Start");
         else
            comment.setText("Use \"Go\" to Start");
      }
      
      gameThread.start();

   }
   
   /**
    * Zwraca pasek menu zawierający menu Sterowanie (Control) i menu Szybkość (Speed) z dostępnymi poleceniami
    * dla tego programu Pentominos.
    * @param includeSaveAndQuit // Jeśli true, to polecenia Zapisz obraz i Wyjdź są zawarte w menu Sterowanie [nie nadaje się do użycia w aplecie].
    */
   public JMenuBar getMenuBar(boolean includeSaveAndQuit, PentominosPanel getOptionsFromThisOne) {
      JMenuBar bar = new JMenuBar();
      JMenu control = new JMenu("Control");
      control.add(goAction);
      control.add(pauseAction);
      control.add(stepAction);
      control.addSeparator();
      control.add(restartAction);
      if (spareSpaces > 0) {
         control.add(restartClearAction);
         control.add(restartRandomAction);
      }
      control.addSeparator();
      control.add(checkForBlocksChoice);
      control.add(randomizePiecesChoice);
      if (rows*cols >= 60)
         control.add(symmetryCheckChoice);  // Dodaj tylko, jeśli plansza może pomieścić wszystkie 12 elementów.
      control.add(oneSidedAction);
      if (includeSaveAndQuit) {
         control.addSeparator();
         control.add(saveAction);
         control.addSeparator();
         control.add(quitAction);
      }
      bar.add(control);
      JMenu speed = new JMenu("Speed");
      speed.add(speedChoice[0]);
      speed.addSeparator();
      for (int i = 1; i < speedChoice.length; i++)
         speed.add(speedChoice[i]);
      bar.add(speed);
      if (getOptionsFromThisOne != null) {
         gameThread.randomizePieces = getOptionsFromThisOne.randomizePiecesChoice.isSelected();
         randomizePiecesChoice.setSelected(gameThread.randomizePieces);
         gameThread.checkForBlocks = (getOptionsFromThisOne.checkForBlocksChoice.isSelected());
         checkForBlocksChoice.setSelected(gameThread.checkForBlocks);
         if (rows*cols >= 60) {
            gameThread.symmetryCheck = getOptionsFromThisOne.symmetryCheckChoice.isSelected();
            symmetryCheckChoice.setSelected(gameThread.symmetryCheck);
         }
         gameThread.useOneSidedPieces = getOptionsFromThisOne.gameThread.useOneSidedPieces; 
         if (gameThread.useOneSidedPieces)
            oneSidedAction.setText("One Sided [Currently ON]...");
         gameThread.useSideA = getOptionsFromThisOne.gameThread.useSideA;
         for (int i = 0; i < speedChoice.length; i++)
            if (getOptionsFromThisOne.speedChoice[i].isSelected()) {
               speedChoice[i].setSelected(true);
               selectedSpeed = i;
               if (selectedSpeed < 2)
                  delay = 0;
               else
                  delay = speedDelay[selectedSpeed-2];
               break;
            }
      }
      return bar;
   }

   /**
   Zapisz obraz PNG aktualnej planszy w pliku wybranym przez użytkownika.
    */
   private void doSaveImage() {
      BufferedImage image = board.getImage();  // Obraz aktualnie wyświetlany w MosaicPanel.
      JFileChooser fileDialog = new JFileChooser();
      String defaultName = "pentominos_" + rows + "x" + cols + ".png"; // Domyślna nazwa pliku do zapisania.
      File selectedFile = new File(defaultName);
      fileDialog.setSelectedFile(selectedFile);
      fileDialog.setDialogTitle("Save Image as PNG File");
      int option = fileDialog.showSaveDialog(board);  // Przedstawia użytkownikowi okno dialogowe "Zapisz plik".
      if (option != JFileChooser.APPROVE_OPTION)
         return;  // anulowana
      selectedFile = fileDialog.getSelectedFile();  // Plik, który użytkownik wybrał do zapisu.
      if (selectedFile.exists()) {
         int response = JOptionPane.showConfirmDialog(board,
               "The file \"" + selectedFile.getName() + "\" already exists.\nDo you want to replace it?",
               "Replace file?",
               JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
         if (response == JOptionPane.NO_OPTION)
            return; // Użytkownik nie chce zastępować istniejącego pliku.
      }
      try {
         if ( ! ImageIO.write(image,"PNG",selectedFile) )  // To zapisuje obraz do pliku.
            JOptionPane.showMessageDialog(board,"Sorry, it looks like PNG files are not supported!");
      }
      catch (Exception e) {
         JOptionPane.showMessageDialog(board,"Sorry, an error occurred while trying to save the file:\n" + e.getMessage());
      }
   }
   
   private void doOneSidedCommand() { // Wywoływane, gdy użytkownik wybierze polecenie "One Sided" 
      final JRadioButton[][] radioButtons = new JRadioButton[6][2];
      JPanel[][] buttonPanels = new JPanel[6][2];
      boolean[] newUseSideA = gameThread.useSideA == null? new boolean[]{true,true,true,true,true,true} : (boolean[])gameThread.useSideA.clone();
      boolean newUseOneSidedPieces = gameThread.useOneSidedPieces;
      JCheckBox enableCheckBox;
      try {
         Icon icon;
         ClassLoader classLoader = getClass().getClassLoader();
         Toolkit toolkit = Toolkit.getDefaultToolkit();
         for (int i = 0; i < 6; i++) {
            ButtonGroup group = new ButtonGroup();
            for (int j = 0; j < 2; j++) {
               URL imageURL = classLoader.getResource("pics/piece" + i + "_side" + (j+1) + ".png");
               if (imageURL == null)
                  throw new Exception();
               icon = new ImageIcon(toolkit.createImage(imageURL));
               radioButtons[i][j] = new JRadioButton("");
               if (!newUseOneSidedPieces)
                  radioButtons[i][j].setEnabled(false);
               group.add(radioButtons[i][j]);
               buttonPanels[i][j] = new JPanel();
               buttonPanels[i][j].setLayout(new BorderLayout(5,5));
               buttonPanels[i][j].add(radioButtons[i][j], j == 0? BorderLayout.WEST : BorderLayout.EAST);
               JLabel label = new JLabel(icon);
               buttonPanels[i][j].add(label,BorderLayout.CENTER);
               final int k = i, l = j;
               label.addMouseListener(new MouseAdapter() {
                  public void mousePressed(MouseEvent evt) {
                     if (radioButtons[k][l].isEnabled())
                        radioButtons[k][l].setSelected(true);
                  }
               });
            }
            radioButtons[i][ newUseSideA[i]? 0 : 1 ].setSelected(true);
         }
      }
      catch (Exception e) {
         JOptionPane.showMessageDialog(null,"Internal Error!  Can't find pentomino images.\nThe \"One Sided\" command will be disabled.");
         oneSidedAction.setEnabled(false);
         e.printStackTrace();
         return;
      }
      JPanel panel = new JPanel();
      JPanel main = new JPanel();
      JPanel top = new JPanel();
      panel.setLayout(new BorderLayout(10,10));
      panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
      panel.add(main,BorderLayout.CENTER);
      panel.add(top,BorderLayout.NORTH);
      main.setLayout(new GridLayout(6,2,12,6));
      for (int i = 0; i < 6; i++) {
         main.add(buttonPanels[i][0]);
         main.add(buttonPanels[i][1]);
      }
      enableCheckBox = new JCheckBox("Enable One Sided Pieces");
      enableCheckBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            boolean on = ((JCheckBox)evt.getSource()).isSelected();
            for (int i = 0; i < 6; i++) {
               radioButtons[i][0].setEnabled(on);
               radioButtons[i][1].setEnabled(on);
            }
         }
      });
      enableCheckBox.setSelected(newUseOneSidedPieces);
      top.setLayout(new GridLayout(2,1,25,25));
      top.add(enableCheckBox);
      top.add(new JLabel("Select the side of each piece that you want to use:"));
      int answer = JOptionPane.showConfirmDialog(this,panel,"Use One Sided Pieces?",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
      if (answer == JOptionPane.CANCEL_OPTION)
         return;
      newUseOneSidedPieces = enableCheckBox.isSelected();
      if (!newUseOneSidedPieces) {
         oneSidedAction.setText("One Sided [Currently OFF]...");
      }
      else {
         for (int i = 0; i < 6; i++)
            newUseSideA[i] = radioButtons[i][0].isSelected();
         gameThread.useSideA = newUseSideA;
         oneSidedAction.setText("One Sided [Currently ON]...");
      }
      gameThread.useOneSidedPieces = newUseOneSidedPieces;
   }

   
   /**
    * To powinno być wywoływane, aby zakończyć wątek gry tuż przed odrzuceniem tego panelu PentominosPanel. 
    * Jest to używane w klasie ramki, Pentominos.java.
    */
   synchronized public void terminate() {
      gameThread.setMessage(TERMINATE_MESSAGE);
      notify();
      gameThread.doDelay(25);
      board = null;
   }
   

   
   private class GameThread extends Thread {  // To powinno być wywoływane, aby zakończyć wątek gry tuż przed odrzuceniem tego panelu PentominosPanel. Jest to używane w klasie ramki, Pentominos.java.

      int moveCount;        // Ile kawałków zostało dotąd umieszczonych?
      int movesSinceCheck;  // Ile ruchów zostało wykonanych od ostatniego przerysowania planszy, działając z prędkością #1?
      int solutionCount;    // Ile dotychczas znaleziono rozwiązań?

      volatile boolean running;   // Prawda, gdy proces rozwiązywania jest uruchomiony (a nie wstrzymany).

      boolean aborted;  // Używane w funkcji play() do sprawdzenia, czy proces rozwiązania został przerwany przez "restart".

      volatile int message = 0;  // "message" jest używane przez wątek interfejsu użytkownika do przesyłania komunikatów kontrolnych do wątku gry. 
      // Wartość 0 oznacza "no message".
      
      int[][] pieces;  // Pieces to położone kawałki, które mogą być albo bezpośrednią kopią danych o kawałkach, albo kopią z losowym porządkiem.

      volatile boolean randomizePieces;  // Jeśli wartość jest równa „true”, tablica kawałków jest losowo zamieniana na początku rozgrywki.
      volatile boolean checkForBlocks;   // Jeśli wartość jest równa „true”, sprawdzane jest oczywiste blokowanie.
      volatile boolean symmetryCheck;    // Jeśli wartość jest równa „true”, to sprawdzana jest symetria planszy, a jeśli ma jakąkolwiek symetrię,
// niektóre elementy są usuwane z listy, aby uniknąć zbędnych rozwiązań.
      volatile boolean useOneSidedPieces;// Jeśli wartość jest równa „true”, to używana jest tylko jedna strona dwustronnych elementów.
      
       volatile boolean[] useSideA;  //Kiedy useOneSidedPieces jest ustawione na true, ta tablica określa, która strona ma być używana dla każdego elementu dwustronnego.
//Dane dotyczące dwóch stron każdego elementu przechowywane są w side_info.
      
      int[][] blockCheck;  // To jest używane do sprawdzania blokady.
      int blockCheckCt;  // Liczba razy, jakie sprawdzenie blokady zostało uruchomione - używane do kontroli rekurencyjnego zliczania zamiast używania tylko tablicy boolowskiej.
      int emptySpaces; // spareSpaces - (liczba czarnych pól); liczba pól, które będą puste w rozwiązaniu.
      
      int squaresLeftEmpty;  // Kwadraty, które są rzeczywiście pozostawione puste w rozwiązaniu do tej pory.
      
      boolean putPiece(int p, int row, int col) {  // Spróbuj umieścić kawałek na planszy i zwróć true, jeśli pasuje.
         if (board.getColor(row,col) != null)
            return false;
         for (int i = 1; i < 8; i += 2) {
            if (row+pieces[p][i] < 0 || row+pieces[p][i] >= rows || col+pieces[p][i+1] < 0 || col+pieces[p][i+1] >= cols)
               return false;
            else if (board.getColor(row+pieces[p][i],col+pieces[p][i+1]) != null)  // Jedno z potrzebnych pól jest już zajęte.
               return false;
         }
         board.setColor(row,col,pieceColor[pieces[p][0]]);
         for (int i = 1; i < 8; i += 2)
            board.setColor(row + pieces[p][i], col + pieces[p][i+1], pieceColor[pieces[p][0]]);
         return true;
      }
      
      void removePiece(int p, int row, int col) { // usuń p z planszy, na pozycji (row,col)
         board.setColor(row,col,null);
         for (int i = 1; i < 9; i += 2) {
            board.setColor(row + pieces[p][i], col + pieces[p][i+1], null);
         }
      }
      
      void play(int row, int col) {   //Rekurencyjna procedura, która próbuje rozwiązać łamigłówkę. 
          //Parametr "square" to numer kolejnego pustego pola do wypełnienia. 
          //Ta procedura obsługuje wszystkie szczegóły dotyczące prędkości, pauzy i kroków rozwiązywania łamigłówki.
         for (int p=0; p<pieces.length; p++) {
            if (!aborted && (used[pieces[p][0]] == false)) {
               if (!putPiece(p,row,col))
                  continue;
               if (checkForBlocks && obviousBlockExists()) {
                  removePiece(p,row,col);
                  continue;
               }
               used[pieces[p][0]] = true;  //Blokuje ponowne użycie tej części na planszy.
               numused++;
               moveCount++;
               movesSinceCheck++;
               boolean stepping = false;
               if (message > 0) {  // Test na "wiadomości" generowane przez działania użytkownika.
                  if (message == PAUSE_MESSAGE || message == STEP_MESSAGE) {
                     stepping = true;
                     if (running && delay == 0)
                        board.forceRedraw();
                     running = false;
                     saveAction.setEnabled(true);
                     setMessage(0);
                  }
                  else if (message >=  RESTART_MESSAGE) {
                     aborted = true;
                     return;  
                  }
                  else { 
                     running = true;
                     saveAction.setEnabled(false);
                     board.setAutopaint( selectedSpeed > 1 );
                     comment.setText("Solving...");
                     setMessage(0);
                  }
               }
               if (numused == piecesNeeded) {  // Znaleziono rozwiązanie
                  solutionCount++;
                  if (delay == 0)
                     board.forceRedraw();  // Jeśli board.autopaint jest wyłączony w tym przypadku, to wymuś pokazanie planszy na ekranie.
                  if (selectedSpeed == 0) {
                     comment.setText("Solution #" + solutionCount + "...  (" + moveCount + " moves)");
                     doDelay(50);  // W prędkości 0, zatrzymaj się tylko na chwilę, gdy rozwiązanie zostanie znalezione.
                  }
                  else {
                     stepAction.setEnabled(true);
                     pauseAction.setEnabled(false);
                     running = false;
                     saveAction.setEnabled(true);
                     comment.setText("Solution #" + solutionCount + "  (" + moveCount + " moves)");
                     doDelay(-1);  // Czekaj czas nieokreślony na polecenie użytkownika w celu ponownego uruchomienia rozwiązania, kroku itp.
                     running = true;
                     board.setAutopaint( selectedSpeed > 1 );
                     saveAction.setEnabled(false);
                     comment.setText(stepping? "Paused." : "Solving...");
                  }
               }
               else {
                  if (stepping) {  // Pauza po umieszczeniu kawałka.
                     comment.setText("Paused.");
                     if (delay == 0)
                        board.forceRedraw();
                     doDelay(-1);  // Czekaj nieskończoność czasu na polecenie.
                  }
                  else if (delay > 0)
                     doDelay(delay);
                  if (movesSinceCheck >= 1000 && !stepping) {
                     if (selectedSpeed == 1) {
                        board.forceRedraw();  // W prędkości 1, ustawienie board.autopaint na false; wymuś ponowne odrysowanie co 1000 ruchów.
                        doDelay(20);
                     }
                     movesSinceCheck = 0;
                  }
                  int nextRow = row;  // Znajdź następne puste miejsce, przechodząc z lewej na prawą, a następnie z góry na dół.
                  int nextCol = col;
                  while (board.getColor(nextRow,nextCol) != null) { // Znajdź następny pusty kwadrat.
                     nextCol++;
                     if (nextCol == cols) {
                        nextCol = 0;
                        nextRow++;
                        if (nextRow == row)  // Przekroczyliśmy koniec planszy!
                           throw new IllegalStateException("Internal Error -- moved beyond end of board!");
                     }
                  }
                  play(nextRow, nextCol);  // spróbuj ukończyć rozwiązanie
                  if (aborted)
                     return;
               }
               removePiece(p,row,col);  // backtrack
               numused--;
               used[pieces[p][0]] = false;
            }
         }
         // Nie można położyć kawałka na (wiersz, kolumna), ale być może można go zostawić puste.
         if (squaresLeftEmpty < emptySpaces) { 
            if (aborted)
               return;
            squaresLeftEmpty++;
            int nextRow = row;  // Szukaj kolejnego pustego miejsca, idąc od lewej do prawej, a następnie od góry do dołu.
            int nextCol = col;
            do { // Znajdź kolejny pusty kwadrat.
               nextCol++;
               if (nextCol == cols) {
                  nextCol = 0;
                  nextRow++;
                  if (nextRow == row)  // Przekroczyliśmy koniec planszy!
                     return;
               }
            } while (board.getColor(nextRow,nextCol) != null);
            play(nextRow, nextCol);  // spróbuj ukończyć rozwiązanie
            squaresLeftEmpty--;
         }
      }
      
      boolean obviousBlockExists() { // Sprawdź, czy plansza ma obszar, który nigdy nie może być wypełniony ze względu na ilość kwadratów, które zawiera.
         blockCheckCt++;
         int forcedEmptyCt = 0;
         for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
               int blockSize = countEmptyBlock(r,c);
               if (blockSize % 5 == 0)
                  continue;
               forcedEmptyCt += blockSize % 5;
               if (forcedEmptyCt > emptySpaces)
                  return true;
            }
         return false;
      }
      
      int countEmptyBlock(int r, int c) {  // Znajdź rozmiar jednego pustego obszaru na planszy; rekurencyjna procedura wywoływana przez obviousBlockExists.
         if (blockCheck[r][c] == blockCheckCt || board.getColor(r,c) != null)
            return 0;
         int c1 = c, c2 = c;
         while (c1 > 0 && blockCheck[r][c1-1] < blockCheckCt && board.getColor(r,c1-1) == null)
            c1--;
         while (c2 < cols-1 && blockCheck[r][c2+1] < blockCheckCt && board.getColor(r,c2+1) == null)
            c2++;
         for (int i = c1; i <= c2; i++)
            blockCheck[r][i] = blockCheckCt;
         int ct = c2 - c1 + 1;
         if (r > 0)
            for (int i = c1; i <= c2; i++)
               ct += countEmptyBlock(r-1,i);
         if (r < rows-1)
            for (int i = c1; i <= c2; i++)
               ct += countEmptyBlock(r+1,i);
         return ct;
      }
      
      void setUpRandomBoard() { // Przygotuj losową planszę, czyli losowo wybierz kwadraty, które zostaną pozostawione puste.
         clickCt = spareSpaces;
         board.clear();
         creatingBoard = false;
         if (spareSpaces == 0)
            return;  // Części wypełnią całą planszę, więc nie ma pustych miejsc do wyboru.
         int x,y;
         int placed = 0;
         int choice = (int)(3*Math.random());
         switch (choice) {
         case 0: // losowo
            for (int i=0; i < spareSpaces; i ++) {
               do {
                  x = (int)(cols*Math.random());
                  y = (int)(rows*Math.random());
               } while (board.getColor(y,x) != null);
               board.setColor(y,x,emptyColor);
            }
            break;
         case 1: // symetrycznie losowo
            while (placed < spareSpaces) {
               x = (int)(cols*Math.random());
               y = (int)(rows*Math.random());
               if (board.getColor(y,x) == null) {
                  board.setColor(y,x,emptyColor);
                  placed++;
               }
               if (placed < spareSpaces && board.getColor(y,cols-1-x) == null) {
                  board.setColor(y,cols-1-x,emptyColor);
                  placed++;
               }
               if (placed < spareSpaces && board.getColor(rows-1-y,x) == null) {
                  board.setColor(rows-1-y,x,emptyColor);
                  placed++;
               }
               if (placed < spareSpaces && board.getColor(rows-1-y,cols-1-x) == null) {
                  board.setColor(rows-1-y,cols-1-x,emptyColor);
                  placed++;
               }
            }
            break;
         default: // losowy blok
            int blockrows;
         int blockcols;
         if (spareSpaces < 4) {
            blockrows = 1;
            blockcols = spareSpaces;
         }
         else if (spareSpaces == 4) {
            blockrows = 2;
            blockcols = 2;
         }
         else {
            blockcols = (int)Math.sqrt(spareSpaces);
            if (blockcols > cols)
               blockcols = cols;
            blockrows = spareSpaces / blockcols;
            if (blockrows*blockcols < spareSpaces)
               blockrows++;
         }
         x = (int)((cols - blockcols+ 1)*Math.random());
         y = (int)((rows - blockrows + 1)*Math.random());
         for (int r = 0; r < blockrows; r++)
            for (int c = 0; c < blockcols && placed < spareSpaces; c++) {
               board.setColor(y+r,x+c,emptyColor);
               placed++;
            }
         break;
         }
      }
      
      private int checkSymmetries(boolean allowFlip) {  // Zwróć kod typu symetrii wyświetlanej na planszy.
         boolean H, V, D1, D2, R90, R180;
         boolean[][] empty = new boolean[rows][cols];
         for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
               empty[i][j] = (board.getColor(i,j) == null);
         if (!allowFlip)
            V = false;
         else {
            V = true;
            VLOOP: for (int i = 0; i < rows; i++)
               for (int j = 0; j < cols/2; j++)
                  if (empty[i][j] != empty[i][cols-j-1]) {
                     V = false;
                     break VLOOP;
                  }
         }
         if (rows != cols)
            R90 = false;
         else {
            R90 = true;
            R90LOOP: for (int i = 0; i < rows-1; i++)
               for (int j = 0; j < cols; j++)
                  if (empty[i][j] != empty[rows-j-1][i]) {
                     R90 = false;
                     break R90LOOP;
                  }
         }
         if (R90) { // Jeśli plansza jest symetryczna względem obrotu o 90 stopni, to jedynymi możliwościami są symetria 8-kierunkowa lub czysta symetria obrotowa.
            if (V)
               return SYMMETRY_ALL;
            else
               return SYMMETRY_R90;
         }
         if (!allowFlip)
            H = false;
         else {
            H = true;
            HLOOP: for (int i = 0; i < rows/2; i++)
               for (int j = 0; j < cols; j++)
                  if (empty[i][j] != empty[rows-i-1][j]) {
                     H = false;
                     break HLOOP;
                  }
         }
         R180 = true;
         R180LOOP: for (int i = 0; i < rows; i++)
            for (int j = 0; j < (cols+1)/2; j++)
               if (empty[i][j] != empty[rows-i-1][cols-j-1]) {
                  R180 = false;
                  break R180LOOP;
               }
         if (!allowFlip || (rows != cols)) 
            D1 = D2 = false;
         else {
            D1 = true;
            D1LOOP: for (int i = 1; i < rows; i++)
               for (int j = 0; j < i; j++)
                  if (empty[i][j] != empty[j][i]) {
                     D1 = false;
                     break D1LOOP;
                  }
            D2 = true;
            D2LOOP: for (int i = 0; i < rows-1; i++)
               for (int j = 0; j < rows-i-1; j++)
                  if (empty[i][j] != empty[rows-j-1][rows-i-1]) {
                     D2 = false;
                     break D2LOOP;
                  }
         }
         if (D1) { // Jeśli plansza jest symetryczna względem 90-stopniowego obrotu, to nie może również posiadać symetrii horyzontalnej (H) ani wertykalnej (V), 
             //ponieważ wtedy symetria obrotowa o 90 stopni (R90) również byłaby prawdziwa.
            if (D2)
               return SYMMETRY_D1D2;
            else
               return SYMMETRY_D1;
         }
         else if (H) { // nie możę też być D2, ponieważ wtedy symetria obrotowa o 90 stopni (R90) również byłaby prawdziwa.
            if (V)
               return SYMMETRY_HV;
            else
               return SYMMETRY_H;
         }
         else if (D2)
            return SYMMETRY_D2;
         else if (V)
            return SYMMETRY_V;
         else if (R180)
            return SYMMETRY_R180;
         else
            return SYMMETRY_NONE;
      }
      
      synchronized void doDelay(int milliseconds) {
//Ta funkcja czeka przez określony czas, który jest podany w milisekundach. 
//Jeśli czas jest ujemny (mniejszy od zera), to oznacza, że oczekiwanie będzie trwać bez określonego limitu czasowego, 
//aż zostanie wysłana wiadomość kontrolna za pomocą funkcji setMessage(). 
//Jeśli czas jest dodatni, to oczekiwanie będzie trwać przez ten określony czas, chyba że zostanie wysłana wiadomość kontrolna wcześniej.
         if (milliseconds < 0) {
            try {
               wait();
            }
            catch (InterruptedException e) {
            }
         }
         else {
            try {
               wait(milliseconds);
            }
            catch (InterruptedException e) {
            }
         }
      }
      
      
      synchronized void setMessage(int message) {  // Wysyła wiadomość kontrolną do wątku gry.
         this.message = message;
         if (message > 0)
            notify();  // Wybudza wątek gry, jeśli jest zatrzymany lub oczekuje na wiadomość (w metodzie doDelay).
      }

      
      /**
       * Metoda run dla wątku, który zarządza rozgrywką.
       */
      public void run() { 
         while (true) {
            try {
               running = false;
               saveAction.setEnabled(true);
               board.repaint();
               while (message != GO_MESSAGE && message != TERMINATE_MESSAGE) {  // Czekaj na konfigurację gry.
                  if (message == RESTART_RANDOM_MESSAGE) {
                     setUpRandomBoard();
                     comment.setText("Solving...");
                     creatingBoard = false;
                     setMessage(GO_MESSAGE);
                     doDelay(1000);  // Udziel użytkownikowi szansy na zmianę wyboru
                  }
                  else if (message == RESTART_CLEAR_MESSAGE || message == RESTART_MESSAGE) {
                     clickCt = 0;
                     creatingBoard = spareSpaces > 0;
                     if (message == RESTART_MESSAGE && spareSpaces > 0) {
                        for (int r = 0; r < rows; r++)
                           for (int c = 0; c < cols; c++)
                              if (board.getColor(r,c) != emptyColor)
                                 board.setColor(r,c,null);
                              else
                                 clickCt++;
                        if (spareSpaces > 0 && clickCt == spareSpaces)
                           comment.setText("Use \"Go\" to Start (or click a black square)");
                        else
                           comment.setText("Select Squares or Use \"Go\" to Start");
                     }
                     else {
                        board.clear();
                        if (creatingBoard)
                           comment.setText("Click (up to) " + spareSpaces + " squares");
                        else
                           comment.setText("Use \"Go\" to Start");
                     }
                     setMessage(0);
                     doDelay(-1);  // Czekaj(na wiadomość kontrolną rozpoczęcia gry).
                  }
               }
               if (message == TERMINATE_MESSAGE)
                  break;
               creatingBoard = false;
               running = true;
               saveAction.setEnabled(false);
               board.setAutopaint(delay > 0);
               board.repaint();
               doDelay(25);
               // begin next game
               pauseAction.setEnabled(true);
               stepAction.setEnabled(false);
               comment.setText("Solving...");
               message = 0;
               for (int i=1; i<=12; i++)
                  used[i] = false;
               numused = 0;
               int startRow = 0;  // Reprezentuje lewy górny róg planszy
               int startCol = 0;
               while (board.getColor(startRow,startCol) != null) {
                  startCol++;  // Przesuń wypełnione kwadraty, ponieważ Play(square) zakłada, że kwadrat jest pusty.
                  if (startCol == cols) {
                     startCol = 0;
                     startRow++;
                  }
               }
               moveCount = movesSinceCheck = solutionCount = 0;
               int[][] pieces2use = piece_data;
               if (symmetryCheck || useOneSidedPieces) {
                  long removeMask = 0;
                  if (symmetryCheck) {
                     int symmetryType = checkSymmetries(!useOneSidedPieces);
                     if (symmetryType != SYMMETRY_NONE) {
                        for (int p = 0; p < remove_for_symmetry[symmetryType].length; p++)
                           removeMask = removeMask | (1L << remove_for_symmetry[symmetryType][p]);
                     }
                  }
                  if (useOneSidedPieces) {
                     for (int p = 0; p < 6; p++) {
                        int[] remove_for_one_sided = side_info[p][ useSideA[p]? 1 : 0 ];
                        for (int j = 0; j < remove_for_one_sided.length; j++)
                           removeMask = removeMask | (1L << remove_for_one_sided[j]);
                     }
                  }
                  if (removeMask != 0) {
                     int ct = 0;
                     for (int p = 0; p < 63; p++)
                        if ((removeMask & (1L << p)) != 0)
                           ct++;
                     pieces2use = new int[63-ct][];
                     int j = 0;
                     for (int p = 0; p < piece_data.length; p++)
                        if ((removeMask & (1L << p)) == 0)
                           pieces2use[j++] = piece_data[p];
                  }
               }
               pieces = pieces2use;
               if (randomizePieces) {
                  if (pieces2use == piece_data) { 
                     pieces = new int[pieces2use.length][];
                     for (int i = 0; i < pieces.length; i++)
                        pieces[i] = pieces2use[i];
                  }
                  for (int i = 0; i < pieces.length; i++) {
                     int r = (int)(pieces.length * Math.random());
                     int[] temp = pieces[r];
                     pieces[r] = pieces[i];
                     pieces[i] = temp;
                  }
               }
               board.setAutopaint( selectedSpeed > 1 );
               randomizePiecesChoice.setEnabled(false);
               symmetryCheckChoice.setEnabled(false);
               oneSidedAction.setEnabled(false);
               blockCheck = new int[rows][cols];
               blockCheckCt = 0;
               emptySpaces = spareSpaces - clickCt;
               squaresLeftEmpty = 0;
               aborted = false;
               boolean blocked = false;
               if (checkForBlocks && obviousBlockExists())
                  blocked = true;
               else
                  play(startRow,startCol);   // Wykonuhe rekurencyjny algorytm, który rozwiąże zagadkę.
               if (message == TERMINATE_MESSAGE)
                  break;
               randomizePiecesChoice.setEnabled(true);
               symmetryCheckChoice.setEnabled(true);
               oneSidedAction.setEnabled(true);
               running = false;
               saveAction.setEnabled(true);
               board.setAutopaint(true);
               board.repaint();
               if (!aborted) {
                  pauseAction.setEnabled(false);
                  stepAction.setEnabled(false);
                  if (blocked)
                     comment.setText("Unsolvable because of obvious blocking.");
                  else if (solutionCount == 0)
                     comment.setText("Done. No soutions. " + moveCount + " moves.");
                  else if (solutionCount == 1)
                     comment.setText("Done. 1 solution. " + moveCount + " moves.");
                  else
                     comment.setText("Done. " + solutionCount + " solutions. "+ moveCount + " moves.");
                  if (spareSpaces > 0)
                     creatingBoard = true;
                  doDelay(-1);
               }
               if (message == TERMINATE_MESSAGE)
                  break;
            }
            catch (Exception e) {
               JOptionPane.showMessageDialog(PentominosPanel.this,"An internal error has occurred:\n"+ e + "\n\nRESTARTING.");
               e.printStackTrace();
               board.setAutopaint(true);
               pauseAction.setEnabled(true);
               stepAction.setEnabled(false);
               message = RESTART_MESSAGE;
            }
         } 
      }
      
   } 
   
   
}

