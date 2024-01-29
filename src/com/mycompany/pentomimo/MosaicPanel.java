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
import javax.swing.*;
import java.awt.image.BufferedImage;

/**

Obiekt klasy MosaicPanel reprezentuje siatkę zawierającą wiersze
i kolumny kolorowych prostokątów. Pomiędzy prostokątami może być "spoina".
(Spoina jest rysowana jako jednopikselowy kontur wokół każdego prostokąta.)
Prostokąty są rysowane jako wypukłe prostokąty w stylu 3D. Udostępnione są
metody do pobierania i ustawiania kolorów prostokątów.
*/
public class MosaicPanel extends JPanel {
   private int rows;       // Liczba wierszy prostokątów w siatce.
   private int columns;    // Liczba kolumn prostokątów w siatce.
   private Color defaultColor;   // Kolor używany dla każdego prostokąta, którego kolor
// nie został ustawiony jawnie. To nigdy nie może być null.
   private Color groutingColor;  // Kolor dla konturu między prostokątami. Jeśli jest to null, to nie jest rysowany.
   private boolean alwaysDrawGrouting;  // Kontur jest rysowana wokół prostokątów domyślnie kolorowanych, jeśli ta wartość jest prawdziwa.
   private boolean autopaint = true;  // Jeśli prawda, to gdy kolor kwadratu zostanie ustawiony, repaint jest wywoływane automatycznie.
   private Color[][] grid; // Tablica zawierająca kolory prostokątów.
// Jeśli wystąpi null w tej tablicy, prostokąt jest rysowany w domyślnym kolorze,
// kontur będzie rysowana tylko wokół tego prostokąta, jeśli alwaysDrawGrouting jest prawdziwe.
// Ponadto, prostokąt jest rysowany jako płaski prostokąt, a nie jako prostokąt 3D.
   private BufferedImage OSI;  // Mozaika jest właściwie rysowana tutaj, a następnie obraz jest kopiowany na ekran.
   private boolean needsRedraw;   // Mozaika jest właściwie rysowana tutaj, a następnie obraz jest kopiowany na ekran.
   /**
Konstruktor klasy MosaicPanel tworzy obiekt z 42 wierszami i 42 kolumnami prostokątów,
oraz z preferowaną wysokością i szerokością prostokąta ustawioną na 16.
*/
   public MosaicPanel() {
      this(42,42,16,16);
   }
  /**
Konstruktor klasy MosaicPanel tworzy obiekt z określoną liczbą wierszy i kolumn prostokątów,
oraz z preferowaną wysokością i szerokością prostokąta ustawioną na 16.
*/
   public MosaicPanel(int rows, int columns) {
      this(rows,columns,16,16);
   }
   
   
   public MosaicPanel(int rows, int columns, int preferredBlockWidth, int preferredBlockHeight) {
      this(rows, columns, preferredBlockWidth, preferredBlockHeight, null, 0);
   }

   
   /**
    *  Konstruktor klasy MosaicPanel tworzy obiekt z określoną liczbą wierszy i kolumn prostokątów,
    * oraz z określonym preferowanym rozmiarem prostokąta. Domyślny kolor prostokątów to czarny,
    * kolor konturu to szary, a alwaysDrawGrouting jest ustawione na false.
    * Jeśli określony jest non-null kolor obramowania, to obramowanie tego koloru jest dodawane do panelu,
    * a jego szerokość jest uwzględniana w obliczeniach preferowanego rozmiaru panelu.
    * @param rows mozaika będzie miała taką ilość wierszy prostokątów. Musi to być liczba dodatnia.
    * @param columns mozaika będzie miała taką ilość kolumn prostokątów. Musi to być liczba dodatnia.
    * @param preferredBlockWidth preferowana szerokość mozaiki będzie ustawiona na tę wartość pomnożoną przez liczbę kolumn. 
    * Rzeczywista szerokość jest ustawiana przez komponent zawierający mozaikę, więc może nie być równa preferowanej szerokości. 
    * Rozmiar jest mierzony w pikselach. Wartość nie powinna być mniejsza niż około 5.
    * @param preferredBlockHeight preferowana wysokość mozaiki będzie ustawiona na tę wartość pomnożoną przez liczbę wierszy. 
    * Rzeczywista wysokość jest ustawiana przez komponent zawierający mozaikę, więc może nie być równa preferowanej wysokości. 
    * Rozmiar jest mierzony w pikselach. 
    * Wartość nie powinna być mniejsza niż około 5.
    * @param borderColor jeśli nie jest null, to obramowanie tego koloru jest dodawane do panelu. 
    * Obramowanie jest uwzględniane w obliczeniach preferowanego rozmiaru panelu.
    * @param borderWidth jeśli borderColor nie jest null, to ten parametr określa szerokość obramowania; wartość mniejsza niż 1 jest traktowana jako 1.
    */
   public MosaicPanel(int rows, int columns, int preferredBlockWidth, int preferredBlockHeight, Color borderColor, int borderWidth) {
      this.rows = rows;
      this.columns = columns;
      grid = new Color[rows][columns];
      defaultColor = Color.black;
      groutingColor = Color.gray;
      alwaysDrawGrouting = false;
      setBackground(defaultColor);
      setOpaque(true);
      setDoubleBuffered(false);
      if (borderColor != null) {
         if (borderWidth < 1)
            borderWidth = 1;
         setBorder(BorderFactory.createLineBorder(borderColor,borderWidth));
      }
      else
         borderWidth = 0;
      if (preferredBlockWidth > 0 && preferredBlockHeight > 0)
         setPreferredSize(new Dimension(preferredBlockWidth*columns + 2*borderWidth, preferredBlockHeight*rows + 2*borderWidth));
   }

   /**
    * setDefaultColor (defaultColor). Jeśli c jest null, kolor zostanie ustawiony na czarny.
    * Kiedy mozaika jest tworzona po raz pierwszy, domyślny kolor to czarny. To jest kolor, który jest używany
    * dla prostokątów, których wartość koloru jest null. Takie prostokąty są rysowane jako płaskie, a nie jako 3D prostokąty.
*/
   public void setDefaultColor(Color c) {
      if (c == null)
         c = Color.black;
      if (! c.equals(defaultColor)) {
         defaultColor = c;
         setBackground(c);
         forceRedraw();
      }
   }
   
   
   /**
    *  Zwraca defaultColor, który nie może być null.
    */
   public Color getDefaultColor() {
      return defaultColor;
   }
   
   
   /**
    * Ustawia kolor konturu (grouting), który jest rysowany między prostokątami. 
    * Jeśli wartość jest null, nie rysowana kontur, a prostokąty wypełniają całą siatkę. 
    * Kiedy mozaika jest tworzona po raz pierwszy, kolor konturu to szary.
    */
   public void setGroutingColor(Color c) {
      if (c == null || ! c.equals(groutingColor)) {
         groutingColor = c;
         forceRedraw();
      }
   }
   
   
   /**
    *  Pobiera bieżący kolor konturu, który może być null.
    */
   public Color getGroutingColor(Color c) {
      return groutingColor;
   }
   
   
   /**
    * Ustawia wartość alwaysDrawGrouting. 
    * Jeśli jest to false, to nie jest rysowana kontur wokół prostokątów, którego wartość koloru jest null. 
    * Kiedy mozaika jest tworzona po raz pierwszy, wartość ta wynosi false.
    */
   public void setAlwaysDrawGrouting(boolean always) {
      if (alwaysDrawGrouting != always) {
         alwaysDrawGrouting = always;
         forceRedraw();
      }
   }
   
   
   /**
    *  Pobiera wartość właściwości alwaysDrawGrouting.
    */   
   public boolean getAlwaysDrawGrouting() {
      return alwaysDrawGrouting; 
   }
   
   
   /**
    * Ustawia liczbę wierszy i kolumn w siatce. 
    * Jeśli wartość parametru preserveData to false, to wartości kolorów wszystkich prostokątów w nowej siatce są ustawiane na null. 
    * Jeśli jest to true, to jak najwięcej danych kolorów jest kopiowane z starej siatki.
    */
   public void setGridSize(int rows, 
         int columns, boolean preserveData) {
      if (rows > 0 && columns > 0) {
         Color[][] newGrid = new Color[rows][columns];
         if (preserveData) {
            int rowMax = Math.min(rows,this.rows);
            int colMax = Math.min(columns,this.columns);
            for (int r = 0; r < rowMax; r++)
               for (int c = 0; c < colMax; c++)
                  newGrid[r][c] = grid[r][c];
         }
         grid = newGrid;
         this.rows = rows;
         this.columns = columns;
         forceRedraw();
      }
   }
   
   
   /**
    *  Zwraca liczbę wierszy prostokątów w siatce.
    */
   public int getRowCount() {
      return rows;
   }
   
   
   /**
    *  Zwraca liczbę kolumn prostokątów w siatce.
    */
   public int getColumnCount() {
      return columns;
   }   

   /**
    * Pobiera kolor, który został ustawiony dla prostokąta w określonym wierszu i kolumnie siatki. 
    * Ta wartość może być null, jeśli nie został ustawiony żaden kolor dla tego prostokąta. 
    * (Takie prostokąty są faktycznie wyświetlane przy użyciu defaultColor.) 
    * Jeśli określony prostokąt znajduje się poza siatką, to zwracana jest wartość null.
    */
   public Color getColor(int row, int col) {
      if (row >=0 && row < rows && col >= 0 && col < columns)
         return grid[row][col];
      else
         return null;
   }
   
   
   /**
    * Zwraca składnik czerwony koloru prostokąta w określonym wierszu i kolumnie. 
    * Jeśli ten prostokąt znajduje się poza siatką lub jeśli nie został określony kolor dla prostokąta, 
    * to zostanie zwrócony składnik czerwony defaultColor.
    */
   public int getRed(int row, int col) {
      if (row >=0 && row < rows && col >= 0 && col < columns && grid[row][col] != null)
         return grid[row][col].getRed();
      else
         return defaultColor.getRed();
   }
   
   
   /**
    * Zwraca składnik zielony koloru prostokąta w określonym wierszu i kolumnie. 
    * Jeśli ten prostokąt znajduje się poza siatką lub jeśli nie został określony kolor dla prostokąta, 
    * to zostanie zwrócony składnik zielony defaultColor.
    */
   public int getGreen(int row, int col) {
      if (row >=0 && row < rows && col >= 0 && col < columns && grid[row][col] != null)
         return grid[row][col].getGreen();
      else
         return defaultColor.getGreen();
   }
   
   
   /**
    * Zwraca składnik niebieski koloru prostokąta w określonym wierszu i kolumnie. 
    * Jeśli ten prostokąt znajduje się poza siatką lub jeśli nie został określony kolor dla prostokąta, 
    * to zostanie zwrócony składnik niebieski defaultColor.
    */
   public int getBlue(int row, int col) {
      if (row >=0 && row < rows && col >= 0 && col < columns && grid[row][col] != null)
         return grid[row][col].getBlue();
      else
         return defaultColor.getBlue();
   }
   
   
   /**
    * Ustawia kolor prostokąta w określonym wierszu i kolumnie. 
    * Jeśli prostokąt znajduje się poza siatką, to ta operacja jest po prostu ignorowana. 
    * Kolor może być wartością null. Prostokątki, dla których kolor jest null, 
    * będą wyświetlane w kolorze defaultColor i zostaną pokazane jako płaskie, a nie w formie 3D.
    */
   public void setColor(int row, int col, Color c) {
      if (row >=0 && row < rows && col >= 0 && col < columns) {
         grid[row][col] = c;
         drawSquare(row,col);
      }
   }
   
   
   /**
    * Ustawia kolor prostokąta w określonym wierszu i kolumnie. 
    * Kolor jest określany przez podanie składowych czerwonej, zielonej i niebieskiej koloru. 
    * Wartości te powinny znajdować się w zakresie od 0 do 255, włącznie, i zostaną ograniczone do tego zakresu. 
    * Jeśli prostokąt znajduje się poza siatką, to ta operacja jest po prostu ignorowana.
    */
   public void setColor(int row, int col, int red, int green, int blue) {
      if (row >=0 && row < rows && col >= 0 && col < columns) {
         red = (red < 0)? 0 : ( (red > 255)? 255 : red);
         green = (green < 0)? 0 : ( (green > 255)? 255 : green);
         blue = (blue < 0)? 0 : ( (blue > 255)? 255 : blue);
         grid[row][col] = new Color(red,green,blue);
         drawSquare(row,col);
      }
   }
   
   
   /**
    * Ustaw kolor prostokąta w określonym wierszu i kolumnie. 
    * Kolor jest określany przez podanie składowych barwy (hue), nasycenia (saturation) i jasności (brightness) koloru. 
    * Wartości te powinny znajdować się w zakresie od 0,0 do 1,0, włącznie, i zostaną ograniczone do tego zakresu. 
    * Jeśli prostokąt znajduje się poza siatką, ta operacja jest po prostu ignorowana.
    */
   public void setHSBColor(int row, int col, 
         double hue, double saturation, double brightness) {
      if (row >=0 && row < rows && col >= 0 && col < columns) {
         grid[row][col] = makeHSBColor(hue,saturation,brightness);
         drawSquare(row,col);
      }
   }
   
   
   /**
    * To jest mała narzędziowa procedura służąca do tworzenia koloru na podstawie wartości barwy (hue), nasycenia (saturation) i jasności (brightness). 
    * Te wartości powinny być w zakresie od 0.0 do 1.0, włącznie, i są ograniczane do tego zakresu. 
    * Jest to wygodna metoda do tworzenia kolorów, która używa wartości zmiennoprzecinkowych podwójnej precyzji zamiast zmiennoprzecinkowych.
    */
   public static Color makeHSBColor(
         double hue, double saturation, double brightness) {
      float h = (float)hue;
      float s = (float)saturation;
      float b = (float)brightness;
      h = (h < 0)? 0 : ( (h > 1)? 1 : h );
      s = (s < 0)? 0 : ( (s > 1)? 1 : s );
      b = (b < 0)? 0 : ( (b > 1)? 1 : b );
      return Color.getHSBColor(h,s,b);
   }
   
   
   /**
    * Ustawia wszystkie prostokąty w siatce na określony kolor. 
    * Kolor może być wartością null. W takim przypadku prostokąty zostaną narysowane jako płaskie, 
    * a nie w formie trójwymiarowych prostokątów, w kolorze domyślnym (defaultColor).
    */
   public void fill(Color c) {
      for (int i = 0; i < rows; i++)
         for (int j = 0; j < columns; j++)
            grid[i][j] = c;
      forceRedraw();      
   }
   
   
   /**
    * Ustaw wszystkie prostokąty w siatce na kolor określony przez podane składowe czerwone, zielone i niebieskie. 
    * Te składowe powinny być liczbami całkowitymi w zakresie od 0 do 255 i zostaną ograniczone do tego zakresu.
    */
   public void fill(int red, int green, int blue) {
      red = (red < 0)? 0 : ( (red > 255)? 255 : red);
      green = (green < 0)? 0 : ( (green > 255)? 255 : green);
      blue = (blue < 0)? 0 : ( (blue > 255)? 255 : blue);
      fill(new Color(red,green,blue));
   }
   
   
   /**
    *  Wypełnia wszystkie prostokąty losowo wybranymi kolorami.
    */
   public void fillRandomly() {
      for (int i = 0; i < rows; i++)
         for (int j = 0; j < columns; j++) {
            int r = (int)(256*Math.random());
            int g = (int)(256*Math.random());
            int b = (int)(256*Math.random());
            grid[i][j] = new Color(r,g,b);
         }
      forceRedraw();
   }
   
   
   /**
    *   Czyści mozaikę, ustawiając wszystkie kolory na null.
    */
   public void clear() {
      fill(null);
   }
   

   /**
    * Zwraca bieżącą wartość właściwości "autopaint".
    */
   public boolean getAutopaint() {
      return autopaint;
   }

   /**
    * Ustawia wartość właściwości "autopaint". 
    * Gdy ta właściwość jest ustawiona na true, każde wywołanie jednej z metod setColor automatycznie powoduje odświeżenie tego kwadratu na ekranie. 
    * Jeśli chcesz uniknąć natychmiastowego odświeżenia - na przykład podczas długiej sekwencji ustawień kolorów, 
    * które wszystkie zostaną wyświetlone naraz - możesz ustawić wartość właściwości autopaint na false. 
    * Gdy wartość ta wynosi false, zmiany kolorów są zapisywane w danych dla mozaiki, ale nie są wykonywane na ekranie. 
    * Gdy właściwość autopaint zostanie ponownie ustawiona na true, zmiany zostaną zastosowane, a cała mozaika zostanie odświeżona. 
    * Domyślną wartością tej właściwości jest true.
    */
   public void setAutopaint(boolean autopaint) {
      if (this.autopaint == autopaint)
         return;
      this.autopaint = autopaint;
      if (autopaint) 
         forceRedraw();
   }

   /**
    * Ta metoda może być wywoływana, aby wymusić ponowne narysowanie całej mozaiki. 
    * Jedyny moment, w którym użytkownicy tej klasy mogą potrzebować użyć tej metody, 
    * to gdy właściwość autopaint jest ustawiona na false, a chcemy pokazać wszystkie zmiany, 
    * które zostały dokonane w mozaice, bez resetowania właściwości autopaint na true.
    */
   final public void forceRedraw() {
      needsRedraw = true;
      repaint();
   }

   /**
    * Zwraca obiekt zawierający dane kolorów potrzebne do ponownego narysowania mozaiki. 
    * Obejmuje to defaultColor, groutingColor, liczbę wierszy i kolumn, 
    * kolor każdego prostokąta oraz wartość alwaysDrawGrouting.
    */
   public Object copyColorData() {
      Color[][] copy = new Color[rows][columns];
      if (alwaysDrawGrouting)
         copy[rows-1] = new Color[columns+3];
      else
         copy[rows-1] = new Color[columns+2];
      for (int r = 0; r < rows; r++)
         for (int c = 0; c < columns; c++)
            copy[r][c] = grid[r][c];
      copy[rows-1][columns] = defaultColor;
      copy[rows-1][columns+1] = groutingColor;
      return copy;
   }
   
   
   /**
    * Parametrem tej metody powinien być obiekt utworzony za pomocą metody copyColorData(). 
    * Ta metoda przywraca dane zawarte w obiekcie do siatki. 
    * Może to zmienić rozmiar siatki, kolory w siatce, defaultColor, groutingColor oraz wartość alwaysDrawGrouting. 
    * Jeśli obiekt jest odpowiedniego typu, to zwracana jest wartość true. 
    * W przeciwnym razie zwracana jest wartość false, a żadne zmiany nie są wprowadzane do bieżących danych.
    */
   public boolean restoreColorData(Object data) {
      if (data == null || !(data instanceof Color[][]))
         return false;
      Color[][] newGrid = (Color[][])data;
      int newRows = newGrid.length;
      if (newRows == 0 || newGrid[0].length == 0)
         return false;
      int newColumns = newGrid[0].length;
      for (int r = 1; r < newRows-1; r++)
         if (newGrid[r].length != newColumns)
            return false;
      if (newGrid[newRows-1].length != newColumns+2
            && newGrid[newRows-1].length != newColumns+3)
         return false;
      if (newGrid[newRows-1][newColumns] == null)
         return false;
      rows = newRows;
      columns = newColumns;
      grid = new Color[rows][columns];
      for (int i = 0; i < rows; i++)
         for (int j = 0; j < columns; j++)
            grid[i][j] = newGrid[i][j];
      defaultColor = newGrid[newRows-1][newColumns];
      setBackground(defaultColor);
      groutingColor = newGrid[newRows-1][newColumns+1];
      alwaysDrawGrouting = newGrid[newRows-1].length == 3;
      forceRedraw();
      return true;
   }
   
   /**
    * Podana współrzędna x piksela w panelu mozaikowym jest używana do określenia numeru wiersza prostokąta mozaiki zawierającego ten piksel. 
    * Jeśli współrzędna x znajduje się poza granicami mozaiki, wartość zwracana to -1 lub jest równa liczbie kolumn, 
    * w zależności od tego, czy x znajduje się po lewej czy po prawej stronie mozaiki.
    */
   public int xCoordToColumnNumber(int x) {
      Insets insets = getInsets();
      if (x < insets.left)
         return -1;
      double colWidth = (double)(getWidth()-insets.left-insets.right) / columns;
      int col = (int)( (x-insets.left) / colWidth);
      if (col >= columns)
         return columns;
      else
         return col;
   }
   
   /**
    * Podana współrzędna y piksela w panelu mozaikowym jest używana do określenia numeru kolumny prostokąta mozaiki zawierającego ten piksel. 
    * Jeśli współrzędna y znajduje się poza granicami mozaiki, wartość zwracana to -1 lub jest równa liczbie wierszy, w zależności od tego, 
    * czy y znajduje się powyżej czy poniżej mozaiki.
    */
   public int yCoordToRowNumber(int y) {
      Insets insets = getInsets();
      if (y < insets.top)
         return -1;
      double rowHeight = (double)(getHeight()-insets.top-insets.bottom) / rows;
      int row = (int)( (y-insets.top) / rowHeight);
      if (row >= rows)
         return rows;
      else
         return row;
   }
   
   /**
    * Ta metoda zwraca obiekt BufferedImage, który zawiera rzeczywisty obraz mozaiki. 
    * Jeśli zostanie wywołana przed narysowaniem mozaiki na ekranie, zwracana wartość będzie równa null.
    */
   public BufferedImage getImage() {
      return OSI;
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if ( (OSI == null) || OSI.getWidth() != getWidth() || OSI.getHeight() != getHeight() ) {
         OSI = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
         needsRedraw = true;
      }
      if (needsRedraw) {
         Graphics OSG = OSI.getGraphics();
         for (int r = 0; r < rows; r++)
            for (int c = 0; c < columns; c++)
               drawSquare(OSG,r,c,false);
         OSG.dispose();
         needsRedraw = false;
      }
      g.drawImage(OSI,0,0,null);
   }
   
   private void drawSquare(Graphics g, int row, int col, boolean callRepaint) {
      if (callRepaint && !autopaint)
         return;
      Insets insets = getInsets();
      double rowHeight = (double)(getHeight()-insets.left-insets.right) / rows;
      double colWidth = (double)(getWidth()-insets.top-insets.bottom) / columns;
      int xOffset = insets.left;
      int yOffset = insets.top; 
      int y = yOffset + (int)Math.round(rowHeight*row);
      int h = Math.max(1, (int)Math.round(rowHeight*(row+1))+yOffset - y);
      int x = xOffset + (int)Math.round(colWidth*col);
      int w = Math.max(1, (int)Math.round(colWidth*(col+1))+xOffset - x);
      Color c = grid[row][col];
      g.setColor( (c == null)? defaultColor : c );
      if (groutingColor == null || (c == null && !alwaysDrawGrouting)) {
         if (c == null)
            g.fillRect(x,y,w,h);
         else
            g.fill3DRect(x,y,w,h,true);
      }
      else {
         if (c == null)
            g.fillRect(x+1,y+1,w-2,h-2);
         else
            g.fill3DRect(x+1,y+1,w-2,h-2,true);
         g.setColor(groutingColor);
         g.drawRect(x,y,w-1,h-1);
      }
      if (callRepaint)
         repaint(x,y,w,h);
   }
   
   private void drawSquare(int row, int col) {
      //Ta metoda rysuje określony prostokąt bezpośrednio na aplecie w obrazie poza ekranem i wywołuje repaint, 
      //aby skopiować ten kwadrat na ekran. (wiersz, kolumna) muszą znajdować się w obrębie siatki.
      if (OSI == null)
         repaint();
      else {
         Graphics g = OSI.getGraphics();
         drawSquare(g,row,col,true);
         g.dispose();
      }
   }
   
   
} // koniec klasy MosaicPanel
