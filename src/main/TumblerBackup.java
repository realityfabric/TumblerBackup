package main;

import java.awt.EventQueue;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.AudioPost;
import com.tumblr.jumblr.types.Blog;
import com.tumblr.jumblr.types.ChatPost;
import com.tumblr.jumblr.types.LinkPost;
import com.tumblr.jumblr.types.Photo;
import com.tumblr.jumblr.types.PhotoPost;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.QuotePost;
import com.tumblr.jumblr.types.TextPost;
import com.tumblr.jumblr.types.VideoPost;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import java.awt.GridLayout;
import javax.swing.JTextArea;

public class TumblerBackup {
	// VERSIONS:
	// AP version
	private final String VERSION = "0.1.0";
	// DB version
	private static int DBVERSION = 1;
	
	//Directory to store media (must be followed by '/')
	private final static String MEDIADIR = "media/";

	// API Keys
	private final String CLIENTKEY = "";
	private final String CLIENTSECRET = ""; 
	
	// Jumblr
	private static JumblrClient client;
	
	// DB
	private static Connection conn = null;
	
	private Map<String, Blog> queueMap = new HashMap<String, Blog>();
	private String[] queueList = {};
	
	private static Pattern mediaURLRegex;

	// create panels for BorderLayout
	private JPanel panelLeft;
	private JPanel panelRight;
	private JPanel panelTop;
	private JPanel panelBottom;
	private JPanel panelCenter;
	
	
	private static JFrame frmTumblerBackupV;
	private JTextField textFieldGetBlog;
	private JButton btnGetBlog;
	private JTextArea textAreaBlogInfo;
	private JComboBox<String> queue;
	private JMenu mnHelp;
	private JMenuItem mntmAboutTumblerBackup;
	private JButton btnRemoveBlog;
	private JButton btnIncreasePriority;
	private JButton btnDecreasePriority;
	private JButton btnBackupQueue;
	private JButton btnCancelCurrent;
	private JButton btnCancelAll;

	private JCheckBox checkBoxParseTextForMedia;

	// User Set Boolean Values
	private boolean cancelAll;
	// if true then text posts and captions will be parsed to replace media urls with local filenames and download files
	// TODO: have user set this value
	private static boolean parseTextForMedia;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TumblerBackup window = new TumblerBackup();
					window.frmTumblerBackupV.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @throws IOException 
	 */
	public TumblerBackup() throws IOException {
		// TODO: remove alpha warning
		int option = JOptionPane.showConfirmDialog(null,
				"This software is in Alpha development mode.\n" +
				"Do not use this to download copyrighted or illegal materials.\n" +
				"By continuing to use this software, you agree that I am not responible for any damage that may be caused.",
				"Continue?",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		switch (option) {
		case JOptionPane.YES_OPTION:
			break; // continue loading the program
		case JOptionPane.NO_OPTION:
			System.exit(0); // :(
			break;
		}

		initialize();
	}
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		// TODO: remove debug
		System.out.println("initialize");
		
		// Initialize Tumblr Connection
		try {
			client = new JumblrClient(CLIENTKEY, CLIENTSECRET);
		} catch (IllegalArgumentException e) { // API Keys aren't set properly (or whatever e.getMessage() says)
			JOptionPane.showMessageDialog(null, e.getMessage(), "IllegalArugmentException", JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
			System.exit(-1);
		}
		
		// connect to SQLite db
		connect();
		checkSchema(true);
		
		mediaURLRegex = Pattern.compile("(http:\\/\\/|https:\\/\\/)([a-z0-9]{2,5})?(\\.)([a-zA-Z0-9]{2,5})?(\\.tumblr.com\\/)([a-zA-Z0-9]*\\/)?([a-zA-Z0-9_]*)?(\\.)(mp3|mp4|jpg|png)");

		// Create Form
		frmTumblerBackupV = new JFrame();
		final String defaultWindowTitle = "Tumbler Backup v" + VERSION;
		frmTumblerBackupV.setTitle(defaultWindowTitle);
		frmTumblerBackupV.setBounds(100, 100, 450, 300);
		frmTumblerBackupV.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmTumblerBackupV.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		// menu stuff
		JMenuBar menuBar = new JMenuBar();
		frmTumblerBackupV.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				switch (JOptionPane.showConfirmDialog(
						null, 
						"Are you sure you would like to exit?", 
						"Close", 
						JOptionPane.YES_NO_OPTION))
				{
				case JOptionPane.YES_OPTION:
					if (conn != null) {
						try {
							conn.close();
						} catch (SQLException e) {
							System.err.println(e.getMessage());
						}
					}
					System.exit(0);
					break;
				case JOptionPane.NO_OPTION:
				default:
					// do nothing
					break;
				}
				
			}
		});
		mnFile.add(mntmClose);
		
		mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		mntmAboutTumblerBackup = new JMenuItem("About Tumbler Backup");
		mntmAboutTumblerBackup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(null, "Tumbler Backup\n" +
													"Version: " + VERSION + "\n");
													// TODO: add git repo address
													// TODO: add license
			}
		});
		mnHelp.add(mntmAboutTumblerBackup);
		// end menu stuff
		
		// create panels
		panelLeft = new JPanel();
		panelRight = new JPanel();
		panelTop = new JPanel();
		panelBottom = new JPanel();
		panelCenter = new JPanel();
		
		// set panels to BorderLayout
		frmTumblerBackupV.getContentPane().add(panelLeft, BorderLayout.WEST);
		frmTumblerBackupV.getContentPane().add(panelRight, BorderLayout.EAST);
		frmTumblerBackupV.getContentPane().add(panelTop, BorderLayout.NORTH);
		frmTumblerBackupV.getContentPane().add(panelBottom, BorderLayout.SOUTH);
		frmTumblerBackupV.getContentPane().add(panelCenter, BorderLayout.CENTER);
		
		// set panel layouts
		panelRight.setLayout(new GridLayout(5,1));
		
		// Create Components
		textFieldGetBlog = new JTextField();
		textFieldGetBlog.setToolTipText("Enter a blog name");
		textFieldGetBlog.setColumns(10);
		
		btnGetBlog = new JButton("Add Blog");
		btnIncreasePriority = new JButton("^ Increase Priority"); // makes a blog higher in the queue
		btnIncreasePriority.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String blogname = (String) queue.getSelectedItem();
				int index = queue.getSelectedIndex();
				if (index > 0) {
					queue.removeItemAt(index);
					queue.insertItemAt(blogname, index - 1);
					queue.setSelectedIndex(index - 1);
				} else {
					JOptionPane.showMessageDialog(null, "Blog is already top priority.");
				}
			}
		});
		btnDecreasePriority = new JButton("v Decrease Priority"); // makes a blog lower in the queue
		btnDecreasePriority.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String blogname = (String) queue.getSelectedItem();
				int index = queue.getSelectedIndex();
				if (index < queue.getComponentCount()) {
					// TODO: this breaks really badly in weird ways
					// Sometimes you can't decrease the priority of blogs
					// even if they aren't the last one in the list
					// Warning: setting the if to (index < queue.getComponentCount() + 1) will
					// allow the last one in the list to decrease in priority
					// which causes it to be removed from the list and throws an error
					queue.removeItemAt(index);
					queue.insertItemAt(blogname, index + 1);
					queue.setSelectedIndex(index + 1);
				} else {
					JOptionPane.showMessageDialog(null, "Blog is already last priority.");
				}
			}
		});
		btnRemoveBlog = new JButton("Remove from queue"); // removes blog from queue
		btnRemoveBlog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				queue.removeItemAt(queue.getSelectedIndex());
			}
		});
		
		btnBackupQueue = new JButton("Backup Blog(s)");
		btnBackupQueue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String title = defaultWindowTitle;
				for (int i = 0; i < queue.getItemCount(); i++) {
					String blogname = queue.getItemAt(i);
					
					frmTumblerBackupV.setTitle(title + " - Backing up " + blogname);

					backupPosts(blogname);
				}
				
				frmTumblerBackupV.setTitle(title + " - Backup Complete!");
			}
		});
		
		btnCancelCurrent = new JButton("Stop Current Backup");
		btnCancelCurrent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//TODO: Cancel current
			}
		});
		
		// TODO: implement
		/*
		btnCancelAll = new JButton("Stop All");
		btnCancelAll.addActionListener(new ActionListener() {
			public void action Performed(ActionEvent arg0) {
				
			}
		});
		*/

		checkBoxParseTextForMedia = new JCheckBox("Download Inline Media?");
		checkBoxParseTextForMedia.setSelected(false);
		parseTextForMedia = false;
		checkBoxParseTextForMedia.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (checkBoxParseTextForMedia.isSelected()) {
					parseTextForMedia = true;
				} else {
					parseTextForMedia = false;
				}
			}
		});
		textAreaBlogInfo = new JTextArea();
		textAreaBlogInfo.setEditable(false);
		
		// not fixing warning because it breaks form editor
		queue = new JComboBox(queueList);
		
		// Add Components to Panels
		panelLeft.add(textFieldGetBlog);
		panelLeft.add(btnGetBlog);
		panelLeft.add(checkBoxParseTextForMedia);

		panelCenter.add(textAreaBlogInfo);
		
		panelRight.add(queue);
		panelRight.add(btnIncreasePriority);
		panelRight.add(btnDecreasePriority);
		panelRight.add(btnRemoveBlog);
		panelRight.add(btnBackupQueue);
		
		// Create Action Listeners
		btnGetBlog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String userInput = textFieldGetBlog.getText();
				try {
					Blog blog = client.blogInfo(userInput);
					
					String blogName = blog.getName();
					queue.addItem(blogName);
					queueMap.put(blogName, blog);
					
					queue.setSelectedIndex(queue.getItemCount() - 1);
				} catch (JumblrException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "\"" + userInput + "\" " + e.getMessage(), userInput, JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		
		queue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String selected = (String) queue.getSelectedItem();
				System.out.println(selected);
				Blog blog = queueMap.get(selected);
				
				updateTextAreaBlogInfo(textAreaBlogInfo, blog);
			}
		});
		
	}
	
	private static void backupPosts (String blogname) {
		// TODO: remove debug
		System.out.println("backupPosts: " + blogname);
		Blog blog = client.blogInfo(blogname);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("limit", 20); // get 20 posts at a time
		params.put("offset", blog.getPostCount()); // start at the end
		params.put("reblog_info", true);
		
		String title = frmTumblerBackupV.getTitle();
	
		// offsetReverse: number of posts collected (used for postcount - offsetReverse - 20)
		int offsetReverse = 20;
		int countDiff = 999999;
		do {
			//System.err.println("Do: " + offsetReverse + " | " + countDiff);
			countDiff = blog.getPostCount() - offsetReverse;
			// TODO: remove debug
			System.out.println("backupPosts: " + blog + " | " + blogname + " | remaining posts: " + countDiff);
			
			// update Title
			
			frmTumblerBackupV.setTitle(title + " - " + countDiff + " posts remaining");
			
			if (countDiff < 0) {
				System.err.println("countDiff < 0: " + countDiff);
				params.replace("offset", 0);
				params.replace("limit", Math.abs(countDiff));
			} else {
				params.replace("offset", countDiff);
			}
			
			List<Post> posts = client.blogPosts(blogname, params);
			for (int i = 0; i < posts.size(); i++) {
				insertPost(posts.get(i));
			}
			
			offsetReverse += 20;
		} while (countDiff >= 0);
		
		//backupPosts (blog, blogname, params, 0);
	}

	// offsetReverse: number of posts collected (used for postcount - offsetReverse - 20)
	private static void backupPosts (Blog blog, String blogname, Map<String, Object> params, Integer offsetReverse) {
		// TODO: remove debug
		System.out.println("backupPosts: " + blog + " | " + blogname + " | offsetReverse: " + offsetReverse);
		
		offsetReverse += 20;
		// if the reverse offset would start you at less than 0, only get the remaining posts
		int countDiff = blog.getPostCount() - offsetReverse;
		if (countDiff == 0) {
			System.out.println("Finished backing up: " + blogname);
			System.exit(0);
		} else if (countDiff < 0) { 
			offsetReverse = 0;
			countDiff *= -1;
			params.replace("limit", countDiff);
		}
		params.replace("offset", offsetReverse);
		
		List<Post> posts = client.blogPosts(blogname, params);
		for (int i = 0; i < posts.size(); i++) {
			insertPost(posts.get(i));
		}
		
		backupPosts (blog, blogname, params, offsetReverse);
	}

	// checks to see if tables exists and what version of the db is being used
	// if update is set to true this will attempt to update the db to the correct version
	//	if no tables exist it will create them all
	// returns true if schema is good to use
	// returns false if schema is not good to use
	private static boolean checkSchema (boolean update) {
		// TODO: remove debug
		System.out.println("checkSchema: update = " + update);
		try {
			PreparedStatement ps;
			if (update) {
				ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS version (version INT)");
				ps.execute();
				ResultSet rs = ps.getResultSet();
				if (rs == null || !rs.next()) { // no version is set (there shouldn't be any other data in the db, but don't assume)
					// if there is no version, set version to current version
					System.out.println("No version detected, setting version to " + DBVERSION);
					ps = conn.prepareStatement("INSERT INTO version VALUES (?)");
					// TODO: decide if new db should always start at 1 and then update incrementally to current version
					ps.setInt(1, DBVERSION);
					setupDBVersionX();
				} else { // a version is set
					// check to see if database versions match
					int version = rs.getInt(1);
					if (!(version == DBVERSION)) {
						if (update) {
							// TODO: if they don't match convert db to correct version
						} else {
							// if update is false and versions don't match then db is not good to use
							return false;
						}
					} else {
						// TODO: DBVERSION matches and DB exists 
					}
				}
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		
		
		return true; // TODO return relevant values
	}

	private static void connect () {
		// TODO: remove debug
		System.out.println("connect");
		try {
			String url = "jdbc:sqlite:tumblerbackup.db";
			// JOptionPane.showMessageDialog(null, url);
			conn = DriverManager.getConnection(url);
			
			// JOptionPane.showMessageDialog(null, "Connection to SQLite has been established.", null, JOptionPane.WARNING_MESSAGE);
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), null, JOptionPane.WARNING_MESSAGE);
		}
	}

	private static String downloadFile (String url) throws IOException {
		return downloadFile (url, "");
	}
	// url: internet address of media file
	// extension: optional argument to specify the file extension, without the . (e.g. mp4, mp3, jpg, etc)
	private static String downloadFile (String url, String extension) throws IOException {
		// TODO: remove debug
		System.out.println("downloadFile: " + url + extension);

		String filename = "";
		int startIndex;
		if (url.isEmpty()) {
			System.err.println("No URL supplied");
			return "";
		}
		else if ((startIndex = url.indexOf(".com/")) >= 0) {}
		else if ((startIndex = url.indexOf(".net/")) >= 0) {}
		else if ((startIndex = url.indexOf(".org/")) >= 0) {}
		else {
			System.err.println("URL isn't com, net, or org");
			System.err.println("URL not supported");
			return "";
		}

		filename = url.substring(startIndex + 5); // filename is the same as on the server

		// only get filename, not directories
		while (filename.indexOf("/") != -1) {
			filename = filename.substring(filename.indexOf("/") + 1);
		}
		
		if (!extension.isEmpty()
				&& !filename.endsWith("." + extension)) {
			filename += "." + extension;
		}

		if (!MEDIADIR.isEmpty()) {
			File dir = new File(MEDIADIR);
			if (!dir.exists()) {
				if(dir.mkdir())
					System.out.println("Created directory: " + dir.getAbsolutePath());
				else {
					throw new IOException("Could not create directory: " + dir.getAbsolutePath());
				}
			}
			filename = MEDIADIR + filename;
		}

		// check if file needs to be downloaded, if not then skip download and return filename
		File file = new File(filename);
		if (file.exists()) {
			System.out.println("File already exists: " + filename);
			
			return filename;
		}
		
		BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
		FileOutputStream fileOutputStream = new FileOutputStream(filename);
		byte databuffer[] = new byte[1024];
		int bytesRead;
		
		while ((bytesRead = in.read(databuffer, 0, 1024)) != -1) {
			fileOutputStream.write(databuffer, 0, bytesRead);
		}

		return filename;
	}
	
	private static String downloadAudioFile(String url) throws IOException {
		// TODO: check type of audio file
		String filename = downloadFile(url, "mp3");
		return filename;
	}

	private static String downloadVideo (String url) throws IOException {
		// TODO: remove debug
		System.out.println("downloadVideo: " + url);
		
		int lastIndex = url.lastIndexOf("/");
		String endOfURL = "";
		if (lastIndex != -1) {
			endOfURL = url.substring(lastIndex);
			System.out.println(endOfURL);
		}
		
		if (endOfURL.equals("/240") ||
			endOfURL.equals("/360") ||
			endOfURL.equals("/480") ||
			endOfURL.equals("/720") ||
			endOfURL.equals("/1080") ||
			endOfURL.equals("/4000")) {
			url = url.substring(0, url.lastIndexOf("/"));
		}

		// TODO: check video type
		String filename = downloadFile(url, "mp4");
		return filename;
	}

	private static String getAudioURL (AudioPost post) {
		// TODO: remove debug
		System.out.println("getAudioURL: " + post);
		String embedCode = post.getEmbedCode();
		// get start and end index to get substring containing audio url
		int startIndex = embedCode.indexOf("audio_file=") + "audio_file=".length();
		String substr = embedCode.substring(startIndex);
		int endIndex = startIndex + substr.indexOf("\""); // not all audio urls end in .mp3
		
		// get audio url
		String audioURL = embedCode.substring(startIndex, endIndex);
		// decode URL
		audioURL = audioURL.replaceAll("%3A", ":");
		audioURL = audioURL.replaceAll("%2F", "/");
		
		return audioURL;
	}
	
	private static String getVideoURL (VideoPost post) {
		// TODO: remove debug
		System.out.println("getVideoURL: " + post);
		String url = "";
		try {
			String embedCode = post.getVideos().get(0).getEmbedCode();
			int startIndex = embedCode.indexOf("src=\"") + "src=\"".length();
			int endIndex = embedCode.indexOf("\" type=");
			url = embedCode.substring(startIndex, endIndex);
		} catch (StringIndexOutOfBoundsException e) {
			// TODO: is there a better way to do this?
			System.err.println("Video Type Not Supported");
		}
		
		return url;
	}

	private static void insertPost (Post post)  {
		// TODO: remove debug
		System.out.println("insertPost: " + post);
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(
					"INSERT INTO post VALUES (" +
					"?, " + // id
					"?, " + // type
					"?, " + // reblogged_from_id
					"?," + // blog_username
					"?," + // reblogged_from_username
					"?," + // op_username
					"?," + // tags
					"?" + // timestamp
					")");
			
	
			
			ps.setLong(1, post.getId());
			ps.setString(2, post.getType());
			if (post.getRebloggedFromId() != null) {
				ps.setLong(3, post.getRebloggedFromId());
			} else {
				ps.setLong(3, 0L); // if id == -1 then null i guess
			}
			ps.setString(4, post.getBlogName());
			if (post.getRebloggedFromName() != null) {
				ps.setString(5, post.getRebloggedFromName());
			} else {
				ps.setString(5, null);
			}
			ps.setString(6, post.getAuthorId());
			ps.setString(7, post.getTags().toString());
			ps.setLong(8, post.getTimestamp());
			
			ps.execute();

			switch (post.getType()) {
			case "note": // iirc answer posts were defined as note posts
			case "answer":
				insertAnswerPost(post);
				break;
			case "audio":
				insertAudioPost(post);
				break;
			case "chat":
				insertChatPost(post);
				break;
			case "link":
				insertLinkPost(post);
				break;
			case "photo":
			case "photoset":
				insertPhotoPost(post);
				break;
			case "quote":
				insertQuotePost(post);
				break;
			case "text":
				insertTextPost(post);
				break;
			case "video":
				insertVideoPost(post);
				break;
			default:
				System.err.println("Post Type " + post.getType() + " unsupported");
				break;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			if (e.getErrorCode() == 19) { // ErrorCode 19: Primary Key constraint failure
				System.err.println("Post Already Exists In DB: " + post.getId());
			} else {
				e.printStackTrace();
			}
		} 
	}

	private static void insertAnswerPost (Post post) {
		AnswerPost ap = (AnswerPost) post;
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO answerpost VALUES (" +
					"?," + // id
					"?," + // asking name
					"?," + // question
					"?" + // answer
					")");

			ps.setLong(1, ap.getId());
			ps.setString(2, ap.getAskingName());
			ps.setString(3, ap.getQuestion());

			String answer = ap.getAnswer();
			if (parseTextForMedia) {
				answer = convertText(answer);
			}
			ps.setString(4, ap.getAnswer());
			
			ps.execute();
			
		} catch (SQLException e) {
			// TODO: Auto-generated code block
			e.printStackTrace();
		}
	}

	private static void insertAudioPost(Post post) {
		AudioPost ap = (AudioPost) post;
		
		try {
			String url = getAudioURL(ap);
			String audio_filename = "";
			String album_art_filename = "";
			try {
				audio_filename = downloadAudioFile(url);
			} catch (NullPointerException e) { // this shouldn't happen
											   // but i'd like things to continue nonetheless
				System.err.println("Could not download audio: " + post);
			}
			try {
				album_art_filename = downloadFile(ap.getAlbumArtUrl());
			} catch (NullPointerException e) {
				System.err.println("Could not download album art: " + post);
			}
			PreparedStatement ps = conn.prepareStatement("INSERT INTO audiopost VALUES (" +
					"?," + // id
					"?," + // album art location
					"?," + // album name
					"?," + // artist name
					"?," + // caption
					"?," + // track name
					"?," + // track number
					"?," + // track location on disk
					"?" + // year
					")");
			
			ps.setLong(1, ap.getId());
			ps.setString(2, album_art_filename);
			ps.setString(3, ap.getAlbumName());
			ps.setString(4, ap.getArtistName());

			String caption = ap.getCaption();
			if (parseTextForMedia) {
				caption = convertText(caption);
			}
			ps.setString(5, caption);
			ps.setString(6, ap.getTrackName());
			ps.setInt(7, ap.getTrackNumber());
			ps.setString(8, audio_filename);
			ps.setInt(9, ap.getYear());
			
			ps.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NullPointerException e2) {
			// TODO: auto-generated catch block
			e2.printStackTrace();
		}
	}

	// this method might never get used
	// it looks like Tumblr swapped all ChatPosts to TextPosts instead
	// gonna be hard to test
	private static void insertChatPost(Post post) {
		ChatPost cp = (ChatPost) post;
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO chatpost VALUES (" +
					"?," + // id
					"?," + // title
					"?" + // text
					")");

			ps.setLong(1, cp.getId());
			ps.setString(2, cp.getTitle());

			String body = cp.getBody();
			if (parseTextForMedia) {
				body = convertText(body);
			}
			ps.setString(3, body);

			ps.execute();
		} catch (SQLException e) {
			// TODO: auto-generated code block
			e.printStackTrace();
		}
	}

	private static void insertLinkPost(Post post) {
		LinkPost lp = (LinkPost) post;
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO linkpost VALUES (" +
					"?," + // id
					"?," + // title
					"?," + // description
					"?" + // link url
					")");

			ps.setLong(1, lp.getId());
			ps.setString(2, lp.getTitle());
			String description = lp.getDescription();
			if (parseTextForMedia) {
				description = convertText(description);
			}
			ps.setString(3, description);
			ps.setString(4, lp.getLinkUrl());

			ps.execute();
		} catch (SQLException e) {
			// TODO: auto-generated code block
			e.printStackTrace();
		}
	}

	private static void insertPhotoPost(Post post) {
		// TODO: remove debug
		System.out.println("insertPhotoPost: " + post);
		PhotoPost pp = (PhotoPost) post;
		List<Photo> photos = pp.getPhotos();
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO photopost VALUES (" +
					"?," + // id
					"?" + // caption
					")");

			ps.setLong(1, pp.getId());
			String caption = pp.getCaption();
			if (parseTextForMedia) {
				caption = convertText(caption);
			}
			ps.setString(2, caption);

			ps.execute();

			for (int i = 0; i < photos.size(); i++) {
				insertPhotos(pp, photos);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void insertPhoto (Post post, Photo photo) {
		// TODO: remove debug
		System.out.println("insertPhoto: " + post + " | " + photo);
		String url = photo.getOriginalSize().getUrl();
		try {
			String filename = downloadFile(url);

			PreparedStatement ps = conn.prepareStatement("INSERT INTO photo (post_id, caption, path) VALUES (" +
					"?," + // post_id
					"?," + // caption
					"?" + // photo location
					")");

			ps.setLong(1, post.getId());
			String caption = photo.getCaption();
			if (parseTextForMedia) {
				caption = convertText(caption);
			}
			ps.setString(2, caption);
			ps.setString(3, filename);

			ps.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void insertPhotos (Post post, List<Photo> photos) {
		// TODO: remove debug
		System.out.println("insertPhotos: " + post);
		for (int i = 0; i < photos.size(); i++) {
			insertPhoto(post, photos.get(i));
		}
	}
	
	private static void insertQuotePost(Post post) {	
		// TODO: remove debug
		System.out.println("insertQuotePost: " + post);
		QuotePost qp = (QuotePost) post;
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO quotepost VALUES (" +
					"?," + // id
					"?," + // source
					"?" + // quote
					")");

			ps.setLong(1, qp.getId());

			String source = qp.getSource();
			if (parseTextForMedia) {
				source = convertText(source);
			}
			ps.setString(2, source);

			String text = qp.getText();
			if (parseTextForMedia) {
				text = convertText(text);
			}
			ps.setString(3, text);

			ps.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void insertTextPost(Post post) {	
		// TODO: remove debug
		System.out.println("insertTextPost: " + post);
		TextPost tp = (TextPost) post;
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO textpost VALUES (" +
					"?," + // id
					"?," + // body
					"?" + // title
					")");

			ps.setLong(1, post.getId());

			String body = tp.getBody();
			if (parseTextForMedia) {
				body = convertText(body);
			}
			ps.setString(2, body);
			ps.setString(3, tp.getTitle());

			ps.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void insertVideoPost(Post post) {
		// TODO: remove debug
		System.out.println("insertVideoPost: " + post);
		VideoPost vp = (VideoPost) post;
		String url = getVideoURL(vp);		
		try {
			String filename = downloadVideo(url);

			PreparedStatement ps = conn.prepareStatement("INSERT INTO videopost VALUES (" +
					"?," + // id
					"?," + // caption
					"?" + // video file name
					")");

			ps.setLong(1, vp.getId());

			String caption = vp.getCaption();
			if (parseTextForMedia) {
				caption = convertText(caption);
			}
			ps.setString(2, caption);
			ps.setString(3, filename);

			ps.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private static void setupDBVersionX () {
		// TODO: remove debug
		System.out.println("setupDBVersionX");
		switch (DBVERSION) {
		case 1:
			setupDBVersion1();
			break;
		default:
			// throw?
			System.err.println("Invalid DBVERSION");
			break;
		}
	}

	private static void setupDBVersion1 () {
		// TODO: remove debug
		System.out.println("setupDBVersion1");
		// TODO: setup
		String createUserTable = "CREATE TABLE IF NOT EXISTS user ("
				+ "username TEXT NOT NULL PRIMARY KEY,"
				+ "title TEXT,"
				+ "description TEXT"
				+ ")";
		
		String createPostTable = "CREATE TABLE IF NOT EXISTS post ("
				+ "id INT NOT NULL PRIMARY KEY,"
				+ "type TEXT NOT NULL," // type of post
				+ "reblogged_from_id INT," // if post was reblogged
				+ "blog_username TEXT NOT NULL,"
				+ "reblogged_from_username TEXT," // can be null?
				+ "op_username TEXT," // original poster, can be null?
				+ "tags TEXT," // tags of post
				+ "timestamp TIMESTAMP,"
				+ "FOREIGN KEY (reblogged_from_id) REFERENCES post(id),"
				+ "FOREIGN KEY (blog_username) REFERENCES user(username),"
				+ "FOREIGN KEY (reblogged_from_username) REFERENCES user(username),"
				+ "FOREIGN KEY (op_username) REFERENCES user(username)"
				+ ")";
		
		String createAnswerPostTable = "CREATE TABLE IF NOT EXISTS answerpost ("
				+ "id INT NOT NULL,"
				+ "asking_name TEXT,"
				+ "question TEXT,"
				+ "answer TEXT,"
				+ "FOREIGN KEY (id) REFERENCES post(id),"
				+ "FOREIGN KEY (asking_name) REFERENCES user(username)"
				+ ")";
		
		String createAudioPostTable = "CREATE TABLE IF NOT EXISTS audiopost ("
				+ "id INT NOT NULL,"
				+ "album_art TEXT," // should point to local file
				+ "album_name TEXT,"
				+ "artist_name TEXT,"
				+ "caption TEXT,"
				+ "track_name TEXT,"
				+ "track_number INT,"
				+ "track TEXT," // should point to local audio file
				+ "year INT,"
				+ "FOREIGN KEY (id) REFERENCES post(id)"
				+ ")";
		
		String createChatPostTable = "CREATE TABLE IF NOT EXISTS chatpost ("
				+ "id INT NOT NULL,"
				+ "title TEXT,"
				+ "dialog TEXT,"
				+ "FOREIGN KEY (id) REFERENCES post(id)"
				+ ")";
		
		String createLinkPostTable = "CREATE TABLE IF NOT EXISTS linkpost ("
				+ "id INT NOT NULL,"
				+ "title TEXT,"
				+ "description TEXT,"
				+ "link_url TEXT,"
				+ "FOREIGN KEY (id) REFERENCES post(id)"
				+ ")";
		
		String createPhotoPostTable = "CREATE TABLE IF NOT EXISTS photopost ("
				+ "id INT NOT NULL,"
				+ "caption TEXT,"
				+ "FOREIGN KEY (id) REFERENCES post(id)"
				+ ")";
		
		// photos which belong to photoposts
		String createPhotoTable = "CREATE TABLE IF NOT EXISTS photo ("
				+ "post_id INT NOT NULL,"
				+ "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," // photos should end up in order from least to greatest, perhaps not sequentially
				+ "caption TEXT,"
				+ "path TEXT," // should point to a local image file
				+ "FOREIGN KEY (post_id) REFERENCES photopost(id)"
				+ ")";
		
		String createQuotePostTable = "CREATE TABLE IF NOT EXISTS quotepost ("
				+ "id INT NOT NULL,"
				+ "source TEXT,"
				+ "quote TEXT,"
				+ "FOREIGN KEY (id) REFERENCES post(id)"
				+ ")";
		
		String createTextPostTable = "CREATE TABLE IF NOT EXISTS textpost ("
				+ "id INT NOT NULL,"
				+ "body TEXT,"
				+ "title TEXT,"
				+ "FOREIGN KEY (id) REFERENCES post(id)"
				+ ")";
		
		String createVideoPostTable = "CREATE TABLE IF NOT EXISTS videopost ("
				+ "id INT NOT NULL,"
				+ "caption TEXT,"
				+ "video TEXT," // should point to a local video file
				+ "FOREIGN KEY (id) REFERENCES post(id)"
				+ ")";
		
		try {
			Statement query = conn.createStatement();
			
			query.execute(createUserTable);
			
			query = conn.createStatement();
			query.execute(createPostTable);
			query.execute(createAnswerPostTable);
			query.execute(createAudioPostTable);
			query.execute(createChatPostTable);
			query.execute(createLinkPostTable);
			query.execute(createPhotoPostTable);
			query.execute(createPhotoTable);
			query.execute(createQuotePostTable);
			query.execute(createTextPostTable);
			query.execute(createVideoPostTable);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void updateTextAreaBlogInfo (JTextArea jta, Blog blog) {
		// TODO: remove debug
		System.out.println("updateTextAreaBlogInfo: " + jta + " | " + blog);
		if (blog != null) {
			String bloginfo = blog.getName() + "\n"
				+ "Title: " + blog.getTitle() + "\n"
				+ "Description: " + blog.getDescription() + "\n"
				+ "Posts: " + blog.getPostCount();
			jta.setText(bloginfo);
		}
	}

	// find all image, audio, and/or video files' urls to download
	private static ArrayList<String> parseText (String text) {
		// TODO: remove debug
		System.out.println("parseText(" + text + ")");

		ArrayList<String> urls = new ArrayList<String>();
		Matcher m = mediaURLRegex.matcher(text);
		int lastMatchPos = 0;
		while (m.find()) {
			urls.add(m.group(0));
			lastMatchPos = m.end();
		}
		if (lastMatchPos != text.length())
		   System.out.println("Invalid string!");

		return urls;
	}

	// replace urls with local file locations
	// (this also downloads the files)
	private static String convertText (String text) {
		ArrayList<String> urls = parseText(text);

		try {
			for (int i = 0; i < urls.size(); i++) {
				String url = urls.get(i);
				String filename = "";
				if (url.endsWith(".mp3"))
					filename = downloadAudioFile(url);
				else if (url.endsWith(".mp4"))
					filename = downloadVideo(url);
				else
					filename = downloadFile(url);

				text = text.replaceAll(url, filename);
			}
		} catch (IOException e) {
			// TODO: auto-generated catch block
			e.printStackTrace();
		}

		return text;
	}
}
