package com.amazon.kindle.app.go;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.amazon.kindle.kindlet.AbstractKindlet;
import com.amazon.kindle.kindlet.KindletContext;
import com.amazon.kindle.kindlet.ui.KImage;
import com.amazon.kindle.kindlet.ui.KindletUIResources;
import com.amazon.kindle.kindlet.ui.KindletUIResources.KColorName;
import com.amazon.kindle.kindlet.ui.image.ImageUtil;

import com.amazon.kindle.app.go.sgf.IncorrectFormatException;
import com.amazon.kindle.app.go.sgf.SGF;
import com.amazon.kindle.app.go.sgf.SGFNode;
import com.amazon.kindle.app.go.sgf.SGFProperty;

public class Main extends AbstractKindlet {
	
	private static final int SQUARE_SIZE = 40;
	private static final int STONE_SIZE  = SQUARE_SIZE;
	private static final int STAR_SIZE = STONE_SIZE/5;
	private static final int GLOBAL_X_OFFSET = 30;
	private static final int GLOBAL_Y_OFFSET = 20;
	private static final int BORDER_WIDTH = 4;
	
	private static final String SGF_DIR = "/sgf/";
	
	private KindletContext context;
	private GoBoard board;
	private Container root;
	/** An image of the current position on the GoBoard */
	private BufferedImage boardImage;
	/** The KImage component containing <code>boardImage</code> */
	private KImage boardComponent;
	
	private final Logger log = Logger.getLogger(Main.class);

	public void create(KindletContext context) {
		this.context = context;
		board = new GoBoard(19);
		board.init();
		
		root = context.getRootContainer();
		boardImage = ImageUtil.createCompatibleImage(board.getSize() * SQUARE_SIZE + STONE_SIZE + GLOBAL_X_OFFSET, board.getSize() * SQUARE_SIZE + STONE_SIZE + GLOBAL_Y_OFFSET, Transparency.TRANSLUCENT);
		Graphics2D g = boardImage.createGraphics();
		g.setColor(context.getUIResources().getBackgroundColor(KindletUIResources.KColorName.WHITE));
		g.fillRect(0, 0, board.getSize() * SQUARE_SIZE, board.getSize() * SQUARE_SIZE);
	
		root.setLayout(new BorderLayout());
		boardComponent = new KImage(boardImage);
		root.add(boardComponent, BorderLayout.NORTH);
		
		BufferedReader sgf = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(SGF_DIR + "sgf24.sgf")));
		String sgfString = "";
		String inLine;
		try {
			inLine = sgf.readLine();

			while (inLine != null) {
				sgfString += inLine;
				inLine = sgf.readLine();
			}

			sgf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info(sgfString);
		SGF sgfParser = new SGF();
		try {
			sgfParser.parseSGF(sgfString);
		} catch (IncorrectFormatException e) {
			e.printStackTrace();
		}
		log.info("Worked!!");
		for (int i = 0; i < sgfParser.getRootTree().getSequence().getNodes().size(); i++) {
			SGFNode node = (SGFNode) sgfParser.getRootTree().getSequence().getNodes().get(i);
			ArrayList properties = node.getProperties();
			Iterator it = properties.iterator();
			while (it.hasNext()) {
				SGFProperty property = (SGFProperty) it.next();
				log.info("Property " + property + " in node #" + i);
				if (property.getIdent().equals("B")) {
					int[] point = GoBoard.convertSGFToCoordinates(property.getValues()[0]);
					board.setPoint(GoBoard.BLACK, point[0], point[1]);
				}
				if (property.getIdent().equals("W")) {
					int[] point = GoBoard.convertSGFToCoordinates(property.getValues()[0]);
					board.setPoint(GoBoard.WHITE, point[0], point[1]);
				}
			}
		}

		drawBoard();
	}

	public void drawBoard() {
		log.info("Drawing board!\n\n");
		Graphics2D g = boardImage.createGraphics();
		
		final int X_OFFSET = STONE_SIZE/2 + GLOBAL_X_OFFSET;
		final int Y_OFFSET = STONE_SIZE/2 + GLOBAL_Y_OFFSET;
		
		g.setColor(context.getUIResources().getColor(KColorName.BLACK));
		// Draw border
		g.fillRect(0 + X_OFFSET, 0 + Y_OFFSET, BORDER_WIDTH, (board.getSize() - 1) * SQUARE_SIZE);
		g.fillRect(0 + X_OFFSET, 0 + Y_OFFSET, (board.getSize() - 1) * SQUARE_SIZE, BORDER_WIDTH);
		g.fillRect(0 + X_OFFSET, (board.getSize() - 1) * SQUARE_SIZE + Y_OFFSET, (board.getSize() - 1) * SQUARE_SIZE, BORDER_WIDTH);
		g.fillRect((board.getSize() - 1) * SQUARE_SIZE + X_OFFSET, 0 + Y_OFFSET, BORDER_WIDTH, (board.getSize() - 1) * SQUARE_SIZE + BORDER_WIDTH);
		
		// Draw grid
		for (int x = 0; x < board.getSize(); x++) {
			g.drawLine(0 + X_OFFSET, x * SQUARE_SIZE + Y_OFFSET, (board.getSize() - 1) * SQUARE_SIZE + X_OFFSET, x * SQUARE_SIZE + Y_OFFSET);
			g.drawLine(x * SQUARE_SIZE + X_OFFSET, 0 + Y_OFFSET, x * SQUARE_SIZE + X_OFFSET, (board.getSize() - 1) * SQUARE_SIZE + Y_OFFSET);
		}
		
		// Draw star points
		int[] starPoints = new int[3];
		switch (board.getSize()) {
		case 19:
			starPoints = new int[] {3, 9, 15};
			break;
		default:
			starPoints = new int[0];
		}
		
		for (int x = 0; x < starPoints.length; x++) {
			for (int y = 0; y < starPoints.length; y++) {
				if (board.getPoint(starPoints[x], starPoints[y]) == GoBoard.BLANK) {
					g.fillOval(X_OFFSET + starPoints[x] * SQUARE_SIZE - STAR_SIZE/2, Y_OFFSET + starPoints[y] * SQUARE_SIZE - STAR_SIZE/2, STAR_SIZE, STAR_SIZE);
				}
			}
		}
		
		// Draw the actual stones
		for (int x = 0; x < board.getSize(); x++) {
			for (int y = 0; y < board.getSize(); y++) {
				switch (board.getPoint(x, y)) {
				case GoBoard.BLACK:
					g.setColor(context.getUIResources().getColor(KColorName.BLACK));
					g.fillOval(X_OFFSET + x * SQUARE_SIZE - STONE_SIZE/2 - 1, Y_OFFSET + y * SQUARE_SIZE - STONE_SIZE/2 - 1, STONE_SIZE+2, STONE_SIZE+2);
					break;
				case GoBoard.WHITE:
					g.setColor(context.getUIResources().getColor(KColorName.BLACK));
					g.fillOval(X_OFFSET + x * SQUARE_SIZE - STONE_SIZE/2, Y_OFFSET + y * SQUARE_SIZE - STONE_SIZE/2, STONE_SIZE, STONE_SIZE);
					g.setColor(context.getUIResources().getColor(KColorName.WHITE));
					g.fillOval(X_OFFSET + x * SQUARE_SIZE - STONE_SIZE/2 + BORDER_WIDTH/2, Y_OFFSET + y * SQUARE_SIZE - STONE_SIZE/2 + BORDER_WIDTH/2, STONE_SIZE - BORDER_WIDTH, STONE_SIZE - BORDER_WIDTH);
					break;
				case GoBoard.BLANK:
					break;
				}
			}
		}
	}
}
