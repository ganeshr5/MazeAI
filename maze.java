import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Maze extends java.applet.Applet implements Runnable {

	int[][] maze;
	String mazeConfFile="mazeConfigurationA.txt";
	String mazeConf;
	final static int backgroundCode = 0;
	final static int wallCode = 1;
	final static int pathCode = 2;		// part of the current path through the maze
	final static int visitedCode = 3;	// has already been explored
	final static int emptyCode = 4;   	// has not been explored
	final static int thiefCode = 5;
	final static int goldCode = 6;
	final static int doorCode = 7;
	final static int securityCode = 8;

	int rows = 12;
	int columns = 12;
	int border = 1;
	int sleepSpeed = 300;
	int nextMaze = 5000;
	Color[] color = new Color[9];

	int thiefPosX = 0;
	int thiefPosY = 0;
	int goldPosX = 0;
	int goldPosY = 0;
	int doorPosX = 0;
	int doorPosY = 0;
	int securityPosX = 0;
	int securityPosY = 0;
	
	/*
	int thiefPosX = 2-1;
	int thiefPosY = 2-1;
	int goldPosX = 9-1;
	int goldPosY = 3-1;
	int doorPosX = 2-1;
	int doorPosY = 11-1;
	int securityPosX = 9-1;
	int securityPosY = 11-1;
	*/

	Thread mazeThread;
	int width = -1;
	int height = -1;
	int totalWidth;
	int totalHeight;
	int left;
	int top;

	boolean mazeExists = false;
	int status = 0;
	final static int GO = 0;
	final static int SUSPEND = 1;
	final static int TERMINATE = 2;

	Integer getIntParam(String paramName) {
		String param = getParameter(paramName);
		if (param == null)
			return null;
		int i;
		try {
			i = Integer.parseInt(param);
		}
		catch (NumberFormatException e) {
			return null;
		}
		return new Integer(i);
	}

	Color getColorParam(String paramName) {
		String param = getParameter(paramName);
		if (param == null || param.length() == 0)
			return null;
		if (param.equalsIgnoreCase("black"))
			return Color.black;
		if (param.equalsIgnoreCase("white"))
			return Color.white;
		if (param.equalsIgnoreCase("red"))
			return Color.red;
		if (param.equalsIgnoreCase("green"))
			return Color.green;
		if (param.equalsIgnoreCase("blue"))
			return Color.blue;
		if (param.equalsIgnoreCase("yellow"))
			return Color.yellow;
		if (param.equalsIgnoreCase("cyan"))
			return Color.cyan;
		if (param.equalsIgnoreCase("magenta"))
			return Color.magenta;
		if (param.equalsIgnoreCase("pink"))
			return Color.pink;
		if (param.equalsIgnoreCase("orange"))
			return Color.orange;
		if (param.equalsIgnoreCase("gray"))
			return Color.gray;
		if (param.equalsIgnoreCase("darkgray"))
			return Color.darkGray;
		if (param.equalsIgnoreCase("lightgray"))
			return Color.lightGray;
		return null;
	}

	public void init() {
		Integer param;
		param = getIntParam("rows");
		if (param != null && param.intValue() > 4 && param.intValue() <= 100) {
			rows = param.intValue();
		}
		param = getIntParam("columns");
		if (param != null && param.intValue() > 4 && param.intValue() <= 100) {
			columns = param.intValue();
		}
		param = getIntParam("border");
		if (param != null && param.intValue() > 0 && param.intValue() <= 100)
			border = param.intValue();
		param = getIntParam("sleepSpeed");
		if (param != null && param.intValue() > 0)
			sleepSpeed = param.intValue();
		param = getIntParam("nextMaze");
		if (param != null && param.intValue() > 0)
			nextMaze = param.intValue();

		color[backgroundCode] = getColorParam("borderColor");
		if (color[backgroundCode] == null)
			color[backgroundCode] = Color.black;
		setBackground(color[backgroundCode]);
		color[wallCode] = getColorParam("wallColor");
		if (color[wallCode] == null) 
			color[wallCode] = Color.gray;
		color[pathCode] = getColorParam("pathColor");
		if (color[pathCode] == null)
			color[pathCode] = Color.blue;
		color[visitedCode] = getColorParam("visitedColor");
		if (color[visitedCode] == null)
			color[visitedCode] = Color.cyan;
		color[emptyCode] = getColorParam("emptyColor");
		if (color[emptyCode] == null)
			color[emptyCode] = Color.white;
			
		color[thiefCode] = getColorParam("thiefColor");
		if (color[thiefCode] == null) 
			color[thiefCode] = Color.red;
		color[goldCode] = getColorParam("goldColor");
		if (color[goldCode] == null) 
			color[goldCode] = Color.yellow;
		color[doorCode] = getColorParam("doorColor");
		if (color[doorCode] == null) 
			color[doorCode] = Color.orange;
		color[securityCode] = getColorParam("securityColor");
		if (color[securityCode] == null) 
			color[securityCode] = Color.magenta;
	}

	void checkSize() { // check the applet size
		if (getWidth() != width || getHeight() != height) {
			width = getWidth();
			height = getHeight();
			int w = (width - 2*border) / columns;	// for width=482, border=1, and columns=12, w=40
			int h = (height - 2*border) / rows;		// for height=482, border=1, and rows=12, h=40
			left = (width - w*columns) / 2;			// then left=1
			top = (height - h*rows) / 2;			// then top=1
			totalWidth = w*columns;					// totalWidth = 480
			totalHeight = h*rows;					// totalHeight = 480
		}
	}

	synchronized public void start() {	// start the thread
		status = GO;
		if (mazeThread == null || ! mazeThread.isAlive()) {
			mazeThread = new Thread(this);
			mazeThread.start();
		}
		else
			notify();
	}

	synchronized public void stop() {
		if (mazeThread != null) {
			status = SUSPEND;
			notify();
		}
	}

	synchronized public void destroy() {
		if (mazeThread != null) {
			status = TERMINATE;
			notify();
		}
	}
	
	synchronized int checkStatus() {
		while (status == SUSPEND) {
			try { wait(); }
			catch (InterruptedException e) { }
		}
		return status;
	}

	public boolean isTerminate() {
		if (checkStatus() == TERMINATE) {
			return true;
		} else
			return false;
	}

	public void update(Graphics g) {
		checkSize();
		redrawMaze(g);
	}

	synchronized void redrawMaze(Graphics g) {	// redraw the entire maze
		g.setColor(color[backgroundCode]);
		g.fillRect(0, 0, width, height);
		if (mazeExists) {
			int w = totalWidth / columns;	// cell width
			int h = totalHeight / rows;		// cell height
			for (int i=0; i<columns; i++) {
				for (int j=0; j<rows; j++) {
					g.setColor(color[maze[i][j]]);
					g.fillRect( (j * w) + left, (i * h) + top, w, h );
				}
			}
		}
	}

	synchronized void updateCell(int row, int col, int colorNum) {
		checkSize();
		int w = totalWidth / columns;	// cell width
		int h = totalHeight / rows;		// cell height
		Graphics gr = getGraphics();
		gr.setColor(color[colorNum]);
		gr.fillRect( (col * w) + left, (row * h) + top, w, h );
		gr.dispose();
	}

	public void run() {
		try { Thread.sleep(2000); }
		catch (InterruptedException e) { }

		while (true) {
			if (isTerminate()) break;
			//mazeConf = readMazeConf(mazeConfFile);
			//createMaze(mazeConf);
			//createMaze1();
			createMaze1();
			
			//createBasicMaze();

			if (isTerminate()) break;

			findGold01(thiefPosX, thiefPosY);
			if (isTerminate()) break;

			synchronized(this) {
				try { wait(nextMaze); }
				catch (InterruptedException e) { }
			}
			if (isTerminate()) break;

			mazeExists = false;
			checkSize();
			Graphics gr = getGraphics();
			redrawMaze(gr);
			gr.dispose();

		}
	}

	String readMazeConf(String filename) {
		String result="";

		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append('\n');
				line = br.readLine();
			}
			mazeConf = sb.toString();
			br.close();
		}
		catch (FileNotFoundException e) {
		//File is not found
		}
		catch (IOException e) {
		//Exception! IOException error occurred
		}

		return result;
	}

	//void createMaze(String mazeInput) {
	void createMaze1() {

		if (maze == null)
		maze = new int[rows][columns];
		int i=0,j=0, m=0, l=0;
	
		String mazeInput=
		"OOOOOOOOOOOOOT  XX    DOO X   XXX  OO    X  X XOOXXX  X X  OO          OO XXXX XX  OO    X X X OO  X X X GSOO XX     XXOO     XX   OOOOOOOOOOOOO";
		
		//"OOOOOOOOOOOOOT  XX    DOO X   XXX  OO    X  X XOOXXX  X X  OO          OO XXXX XX  OO    X X X OO GX X X  SOO XX     XXOO     XX   OOOOOOOOOOOOO";
		//"OOOOOOOOOOOOOT  XXG   DOO X   XXX  OO    X  X XOOXXX  X X  OO          OO XXXX XX  OO    X X X OO  X X X  SOO XX     XXOO     XX   OOOOOOOOOOOOO";

		l=mazeInput.length();
		m=0;
		for (i=0; i<rows; i++) {
			for (j=0; j < columns; j++) {
				switch(mazeInput.charAt(m)){
					case 'O':	maze[i][j] = wallCode;
								break;
					case 'X':	maze[i][j] = wallCode;
								break;
					case ' ':	maze[i][j] = emptyCode;
								break;
					case 'T':	maze[i][j] = thiefCode;
								thiefPosX = i; 
								thiefPosY = j;
								break;
					case 'G':	maze[i][j] = goldCode;
								goldPosX = i; 
								goldPosY = j;
								break;
					case 'D':	maze[i][j] = doorCode;
								doorPosX = i; 
								doorPosY = j;
								break;
					case 'S':	maze[i][j] = securityCode;
								securityPosX = i; 
								securityPosY = j;
								break;
				}
				m++;
			}
		}

		mazeExists = true;
		checkSize();
		if (checkStatus() == TERMINATE)
			return;
			
		Graphics gr = getGraphics();
		redrawMaze(gr);
		gr.dispose();
	}
	
	void createEmptyMaze() {
		if (maze == null)
			maze = new int[rows][columns];
		int i=0,j=0;
		
		for (i=0; i<rows; i++) {
			for (j=0; j < columns; j++) {
				maze[i][j] = wallCode;
			}
		}
			
		for (i=1; i<rows-1; i++) {
			for (j=1; j<columns-1; j++) {
				maze[i][j] = emptyCode;
			}
		}
		
		maze[thiefPosX][thiefPosY] = thiefCode;
		maze[goldPosX][goldPosY] = goldCode;
		maze[doorPosX][doorPosY] = doorCode;
		maze[securityPosX][securityPosY] = securityCode;
		
		mazeExists = true;
		checkSize();
		if (checkStatus() == TERMINATE)
			return;
			
		Graphics gr = getGraphics();
		redrawMaze(gr);
		gr.dispose();
	}
	
	boolean findGold01(int trow, int tcol) {
		if (maze[trow][tcol] == emptyCode || maze[trow][tcol] == thiefCode || maze[trow][tcol] == goldCode || maze[trow][tcol] == doorCode || maze[trow][tcol] == securityCode) {
			maze[trow][tcol] = pathCode;
			if (checkStatus() == TERMINATE) return false;
			
			updateCell(trow, tcol, pathCode);
			if (trow == goldPosX && tcol == goldPosY)
				return true;
				
			try { Thread.sleep(sleepSpeed); }
			catch (InterruptedException e) { }
			
			if ( (findGold01(trow-1, tcol) && checkStatus() != TERMINATE)  ||
			(findGold01(trow, tcol-1) && checkStatus() != TERMINATE)  ||
			(findGold01(trow+1, tcol) && checkStatus() != TERMINATE)  ||
			findGold01(trow, tcol+1) )
				return true;
			if (checkStatus() == TERMINATE) return false;
			
			maze[trow][tcol] = visitedCode;		// gold is not found, backtrack
			updateCell(trow, tcol, visitedCode);
			synchronized(this) {
				try { wait(sleepSpeed); }
				catch (InterruptedException e) { }
			}
			
			if (checkStatus() == TERMINATE) return false;
		}
		return false;
	}
}
