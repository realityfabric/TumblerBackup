# Tumbler Backup
An application to back up your Tumblr Posts

This application has been tested on Windows 10 and Ubuntu 16.04.
Thanks to @cryocat (https://github.com/Cryocrat) for helping me test this on Windows 10.

## Requirements to Run
- Java

## Requirements for Developers
- JDK
- JDE
- Jumblr (https://github.com/tumblr/jumblr)
- SQLite Java Drivers (https://bitbucket.org/xerial/sqlite-jdbc/downloads/)

## Instructions
1. Place the Tumbler Backup application in the folder you would like it to run.
  - The application will create a database file and a folder inside the same folder as the application.
  - Make sure that the disk you run Tumbler Backup on has enough space for the amount of media you expect to be saving.
  - I saw an average of 1GB for every 1000 posts. Your Mileage May Vary.

2. Run Tumbler Backup.
  Windows:
  - You can either Double-Click on it, or you can run it from Command Prompt or Powershell.
  
  Linux:
  - Run it from Terminal.
  - Running it by double-clicking it will cause it to store the database and media folder in your home directory.
  
  Mac:
  - I haven't tested it, but it should run by following instructions for Linux.
  
  If you run Tumbler Backup using CMD, PS, or Terminal, you will be able to see the debug output.
  You can use this if you encounter any bugs to give me more context and help me fix the problem.
  
3. There will be a disclaimer when the application starts.
  - Read the disclaimer, decide if you agree with the terms and select an option.

4. Enter a blog url (with or without '.tumblr.com') into the text field.

5. Click the "Add Blog" button.
  - The blog will be added to a list (viewable by clicking on the drop down menu).
  - Blog details will be displayed in the middle of the window.

6. Continue adding blogs to the list.
  - You can reorder them how you want*, blogs will be backed up in order from top to bottom.

7. If you would like to back up media files which are embedded in the captions (e.g. reaction images):
  - Check the box that says "Download Inline Media?"
  - This will take longer because there will be more to download.

8. When you are ready to backup the blogs in your list, click the "Backup Blog(s)" button.
  - WARNING:
  - You cannot make any changes after you click the "Backup Blog(s)" button until the backup(s) are complete.
  - You will only be able to close the application when it is complete.
  - You can force the application closed with Task Manager or similar,
      but this may cause posts being backed up at that point to become corrupted.
      

The blog currently being backed up will be displayed in the Title bar of the window, as well as the number of posts left to be backed up (note that it only changes every 20 posts).

*Note that there is a bug with the decrease priority button. You can increase the priority of blogs instead if you encounter this.
