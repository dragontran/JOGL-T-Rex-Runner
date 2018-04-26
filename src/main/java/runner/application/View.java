package runner.application;

import java.awt.Component;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;

import runner.application.obstacles.Cloud;
import runner.application.obstacles.Tree;

public class View implements GLEventListener {

	// **********************************************************************
	// Public Class Members
	// **********************************************************************

	public static final GLU GLU = new GLU();
	public static final GLUT GLUT = new GLUT();
	public static final Random RANDOM = new Random();
	public static final int DEFAULT_FRAMES_PER_SECOND = 60;
	public static final double FRAME_TIME_DELTA = 5.0 * 1.0 / (double) DEFAULT_FRAMES_PER_SECOND;
	private static final DecimalFormat FORMAT = new DecimalFormat("0.000");

	// **********************************************************************
	// Private Members
	// **********************************************************************

	// State (internal) variables
	private final GLJPanel canvas;

	// dino model
	private Dino dino;
	public Random rand;

	private int counter = 0; // Just an animation counter
	private int jumpFrameLimit = 0; // Limit for super jump
	private boolean spaceIsPressed = false;
	private int w; // Canvas width
	private int h; // Canvas height
	private double floorLocY = 80.0; //ground location for dino
	private Vector2D gravity = new Vector2D(0.0, -15.0); //gravity vector

	private final FPSAnimator animator;
	private TextRenderer renderer;

	private Point2D.Double origin; // Current origin coordinates
	private Point2D.Double cursor; // Current cursor coordinates

	ArrayList<Cloud> cloudList;
	ArrayList<Tree> treeList;

	// **********************************************************************
	// Constructors and Finalizer
	// **********************************************************************
	public View(GLJPanel canvas) {

		// init dino model info
		Point2D.Double position = new Point2D.Double(200.0, floorLocY + Dino.DEFAULT_HEIGHT);
		Point2D.Double[] polygonPoints = generateDinoPoints(position, Dino.DEFAULT_WIDTH, Dino.DEFAULT_HEIGHT); //generates bounding box for dino
		this.dino = new Dino(position, polygonPoints);
		dino.setCurrentSprite("run0"); //sets initial sprite to run0

		this.canvas = canvas;
		cursor = null;

		// Initialize model
		origin = new Point2D.Double(0.0, 0.0);

		// Initialize rendering
		canvas.addGLEventListener(this);
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		animator.start();

		new KeyHandler(this);

		this.cloudList = new ArrayList<Cloud>();
		rand = new Random();

		addCloud();

		this.treeList = new ArrayList<Tree>();
		addTree();

		// TODO: GAME LOOP
		try {
			for (;;) {

				TimeUnit.SECONDS.sleep(rand.nextInt(5) + 1);
				addCloud();
				addTree();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Point2D.Double[] generateDinoPoints(Point2D.Double center, int w, int h) {
		//generate bounding box for dino
		Point2D.Double[] points = new Point2D.Double[4];
		points[0] = new Point2D.Double(center.x - w/2, center.y - h/2);
		points[1] = new Point2D.Double(center.x - w/2, center.y + h/2);
		points[2] = new Point2D.Double(center.x + w/2, center.y + h/2);
		points[3] = new Point2D.Double(center.x + w/2, center.y - h/2);
		return points;
	}

	private Point2D.Double[] generatePolygon(Point2D.Double center, int sides, int w, int h) {
		Point2D.Double[] polygonPoints = new Point2D.Double[sides];

		// generate points based off circle method
		for (int i = 0; i < sides; i++) {
			double angle = (2 * Math.PI * i) / sides;

			double x = Math.cos(angle) * w;
			double y = Math.sin(angle) * h;

			// add points to array
			polygonPoints[i] = new Point2D.Double(x + center.getX(), y + center.getY());
		}

		return polygonPoints;
	}

	// **********************************************************************
	// Override Methods (GLEventListener)
	// **********************************************************************

	@Override
	public void init(GLAutoDrawable drawable) {
		w = drawable.getWidth();
		h = drawable.getHeight();
		renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 25), true, true);
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		update();
		render(drawable);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		this.w = w;
		this.h = h;
	}

	// **********************************************************************
	// Getters and Setters
	// **********************************************************************
	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	public int getCounter() {
		return counter;
	}

	public boolean isSpacePressed() {
		if (spaceIsPressed) return true;
		return false;
	}

	public void setSpacePressed(boolean state) {
		spaceIsPressed = state;
	}

	public Point2D.Double getOrigin() {
		return new Point2D.Double(origin.x, origin.y);
	}

	public void setOrigin(Point2D.Double origin) {
		this.origin.x = origin.x;
		this.origin.y = origin.y;
		canvas.repaint();
	}

	public Point2D.Double getCursor() {
		return cursor;
	}

	public void setCursor(Point2D.Double cursor) {
		this.cursor = cursor;
		canvas.repaint();
	}

	public Dino getDino() {
		return dino;
	}

	public void setDino(Dino saur) {
		this.dino = saur;
	}

	// set frame limit for super jump
	public void setSuperJumpFrameLimit(int frameLimit) {
		jumpFrameLimit = frameLimit;
	}

	// **********************************************************************
	// Private Methods (Viewport)
	// **********************************************************************

	private void setProjection(GL2 gl) {
		GLU glu = new GLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		// bottom left corner will be (0,0)
		// top right corner will be (1000,450)
		glu.gluOrtho2D(0.0f, 1000.0f, 0.0f, 450.0f);

	}
	// **********************************************************************
	// Private Methods (Rendering)
	// **********************************************************************

	private void update() {
		updateDino();
		counter++; // Counters are useful, right?
	}

	private void updateDino() {
		if (!spaceIsPressed) jumpFrameLimit = counter; //as long as space is not pressed update jump frame limit to be equal to current frame count
		if (counter > jumpFrameLimit) {
			//when the current frame count passes the frame limit for the super jump, set dino to super jump mode and update sprite to crouch
			dino.setJumpType(2);
			dino.setWidth(46);
			dino.setHeight(44);
			Point2D.Double[] polygonPoints = generateDinoPoints(dino.getPosition(), dino.getWidth(), dino.getHeight());
			dino.setPoints(polygonPoints);
			//current sprite determines which crouch frame to start on
			String sprite = dino.getCurrentSprite();
			if (sprite == "run0") dino.setCurrentSprite("crouch0");
			else if (sprite == "run1") dino.setCurrentSprite("crouch1");
		}
		if (counter <= jumpFrameLimit) dino.setJumpType(1); //reset to normal jump

		// if jumping, update dino position and new polygon points
		if (dino.isJumping()) {
			// new_position = old_position + delta_time * current_velocity
			// new_velocity = old_velocity + delta_time * gravity
			double newYPosition = dino.getY() + FRAME_TIME_DELTA * dino.getJumpVelocity().getY();
			double newYVelocity = dino.getJumpVelocity().getY() + FRAME_TIME_DELTA * gravity.y; 																
			dino.setPosition(new Point2D.Double(dino.getX(), newYPosition));
			dino.setJumpVelocity(new Vector2D(dino.getJumpVelocity().getX(), newYVelocity));
			dino.setWidth(Dino.DEFAULT_WIDTH);
			dino.setHeight(Dino.DEFAULT_HEIGHT);
			Point2D.Double[] polygonPoints = generateDinoPoints(dino.getPosition(), dino.getWidth(), dino.getHeight());
			dino.setPoints(polygonPoints);
			dino.setCurrentSprite("jump");
		}

		// if dino goes below floor location then stop dino and set dino to running
		if (dino.getPosition().y - dino.getHeight() < floorLocY) {
			dino.setY(floorLocY + dino.getHeight());
			dino.setWidth(Dino.DEFAULT_WIDTH);
			dino.setHeight(Dino.DEFAULT_HEIGHT);
			Point2D.Double[] polygonPoints = generateDinoPoints(dino.getPosition(), dino.getWidth(), dino.getHeight());
			dino.setPoints(polygonPoints);
			dino.setInJumpState(false);
			dino.setCurrentSprite("run0");
		}
		
		//update dino sprite every 5 frames
		if (counter % 30 == 0) {
			String currentSprite = dino.getCurrentSprite();
			if (currentSprite == "crouch0") dino.setCurrentSprite("crouch1");
			if (currentSprite == "crouch1") dino.setCurrentSprite("crouch0");
			if (currentSprite == "run0") 	dino.setCurrentSprite("run1");
			if (currentSprite == "run1") 	dino.setCurrentSprite("run0"); 
		}
	}

	private void render(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT); // Clear the buffer

		setProjection(gl);

		drawBackground(gl);
		drawGround(gl);
		drawCursorCoordinates(gl); // Draw some text
		drawDino(gl);

		// draw clouds
		drawClouds(gl);

		// move clouds
		moveClouds();

		drawTrees(gl);

		moveTrees();
	}

	// Point2D.Double[] please;
	public List<Point2D.Double> generateTree(double x1, double y1, double angle, double depth, double scale,
			List<Point2D.Double> test) {
		if (depth == 0)
			return null;

		double x2 = x1 + (Math.cos(Math.toRadians(angle)) * depth * scale);
		double y2 = y1 + (Math.sin(Math.toRadians(angle)) * depth * scale);

		test.add(new Point2D.Double(x1, y1));
		test.add(new Point2D.Double(x2, y2));

		generateTree(x2, y2, angle - 20, depth - 1, scale, test);
		generateTree(x2, y2, angle + 20, depth - 1, scale, test);

		return test;
	}

	public Point2D.Double[] generateTreePoints(List<Point2D.Double> test) {
		Point2D.Double[] ok = test.toArray(new Point2D.Double[test.size()]);
		return ok;
	}

	public void addTree() {
		List<Point2D.Double> treePoints = new ArrayList<Point2D.Double>();
		Point2D.Double[] treePointsArray;

		Point2D.Double position = new Point2D.Double(1100, 25.0);

		// TODO: parameterize tree generation
		treePointsArray = generateTreePoints(generateTree(position.getX(), position.getY(), 90, 9, 2.0, treePoints));

		treeList.add(new Tree(position, new Vector2D(-3, 0), treePointsArray));
	}

	public void drawTrees(GL2 gl) {
		for (Tree tree : treeList) {
			Point2D.Double[] please = tree.getPoints();
			gl.glLineWidth((float) 2.0);
			gl.glBegin(GL2.GL_LINES);
			gl.glColor3d(0.392157, 0.247059, 0.0470588f);
			for (int i = 0; i < please.length; i = i + 2) {
				gl.glVertex2d(please[i].getX(), please[i].getY());
				gl.glVertex2d(please[(i + 1) % please.length].getX(), please[(i + 1) % please.length].getY());

			}
			gl.glEnd();
		}
	}

	// **********************************************************************
	// Private Methods (Scene)
	// **********************************************************************
	private void drawCursorCoordinates(GL2 gl) {
		if (cursor == null)
			return;

		String sx = FORMAT.format(new Double(cursor.x));
		String sy = FORMAT.format(new Double(cursor.y));
		String s = "(" + sx + "," + sy + ")";

		renderer.beginRendering(this.getWidth(), this.getHeight());
		renderer.setColor(1.0f, 1.0f, 0, 1.0f);
		renderer.draw(s, 2, 2);
		renderer.endRendering();
	}

	private void drawBackground(GL2 gl) {
		gl.glBegin(GL2.GL_POLYGON);

		gl.glColor3d(0.0470588, 0.192157, 0.4);
		gl.glVertex2f(0, 0);
		gl.glVertex2f(this.getWidth(), 0);

		gl.glColor3d(0.992157, 0.490196, 0.00392157);

		gl.glVertex2f(this.getWidth(), this.getHeight());

		gl.glVertex2f(0, this.getHeight());

		gl.glEnd();
	}

	private void drawGround(GL2 gl) {
		gl.glBegin(GL2.GL_POLYGON);

		int yo = 2 * (this.getHeight() / 3); // 2/3 the height

		gl.glColor3d(0.392157, 0.247059, 0.0470588f);
		gl.glVertex2i(0, yo);
		gl.glVertex2i(this.getWidth(), yo);
		gl.glVertex2i(this.getWidth(), this.getHeight());
		gl.glVertex2i(0, this.getHeight());

		gl.glColor3f(0, 0, 0);

		gl.glEnd();

		int offsetX = 0;

		for (int i = 0; i < 27; i++) {
			offsetX = ((i * 20) - counter % 20) * 2;
			// System.out.println(offsetX);
			drawGroundLine(gl, offsetX, yo);
		}
	}

	private void drawGroundLine(GL2 gl, int dx, int yo) {
		gl.glBegin(GL2.GL_QUADS);

		gl.glColor3d(0.278431, 0.392157, 0.0470588);
		gl.glVertex2i(dx + 30, yo);
		gl.glVertex2i(dx + (-20), this.getHeight());
		gl.glVertex2i(dx + (-10), this.getHeight());
		gl.glVertex2i(dx + 40, yo);

		gl.glEnd();
	}

	// **********************************************************************
	// Public Methods
	// **********************************************************************
	public Component getComponent() {
		// TODO Auto-generated method stub
		return (Component) canvas;
	}

	private void drawDino(GL2 gl) {
		//enable blending to allow for png transparency (for texture drawing only)
		gl.glEnable(GL2.GL_BLEND); 
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

		//enable texture drawing
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);

		//get texture (the current dino sprite) and bind it to set it as the current texture to draw
		Texture texture = dino.getCurrentSpriteImage().getTexture();
		if (texture != null) {
			gl.glBindTexture(GL2.GL_TEXTURE_2D, texture.getTextureObject());
		}

		gl.glBegin(GL2.GL_QUADS); //use quads to draw bounding box

		gl.glColor3d(0.403922, 0.560784, 0);

		Point2D.Double[] points = dino.getPoints();

		//texture coordinates are relative to texture's bounding box and takes values between 0.0 and 1.0
		//so to use full bounding box width and height, texture coordinates must span 0 and 1
		//for sprite facing left, texture coordinate starts in top right and goes counter-clockwise so that sprite is facing right in program
		gl.glTexCoord2d(1, 1);
		gl.glVertex2d(points[0].getX(), points[0].getY());

		gl.glTexCoord2d(1, 0);
		gl.glVertex2d(points[1].getX(), points[1].getY());

		gl.glTexCoord2d(0, 0);
		gl.glVertex2d(points[2].getX(), points[2].getY());

		gl.glTexCoord2d(0, 1);
		gl.glVertex2d(points[3].getX(), points[3].getY());

		gl.glEnd();
		gl.glFlush();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0); //unbind any textures
		gl.glDisable(GL2.GL_TEXTURE_2D); //disable texture drawing
		gl.glDisable(GL2.GL_BLEND); //disable blending
	}

	public void addCloud() {
		Random rand = new Random();
		double x = 1020.0;
		double y = (double) (rand.nextInt(300 + 1 - 20) + 20);

		Point2D.Double position = new Point2D.Double(x, y);
		Vector2D velocity = new Vector2D(-(rand.nextInt(5) + 1), 0);
		Point2D.Double[] cloudPoints = generatePolygon(position, 32, 30, 20);
		cloudList.add(new Cloud(position, velocity, cloudPoints));
	}

	public void drawClouds(GL2 gl) {

		for (Cloud cloud : cloudList) {

			gl.glBegin(GL2.GL_POLYGON);

			gl.glColor3d(0.662745, 0.662745, 0.662745);
			for (Point2D.Double point : cloud.getPoints()) {
				gl.glVertex2d(point.getX(), point.getY());
			}

			gl.glEnd();
		}
	}

	public void moveClouds() {
		// using iterator instead of looping
		// through the array list of cloud object
		Iterator<Cloud> iterator = cloudList.iterator();
		while (iterator.hasNext()) {

			Cloud cloud = iterator.next();

			// move cloud by adding velocity vector to position
			cloud.moveCloud();

			/* TODO: this is the collision test it works.... */
			if (dino.collides(cloud)) {
				iterator.remove();
			}

			// removes clouds that leave the screen
			if (cloud.getPosition().getX() < -15) {
				iterator.remove();
			}
		}
	}

	public void moveTrees() {
		for (Tree tree : treeList) {
			tree.moveTree();

			if (dino.collides(tree.getPoints())) {
				System.out.println("GAME OVER BUT I DONT WANT TO FAIL");
			}
		}
	}
}
