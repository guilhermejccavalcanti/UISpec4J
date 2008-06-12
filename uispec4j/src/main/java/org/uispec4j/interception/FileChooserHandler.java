package org.uispec4j.interception;

import org.uispec4j.Trigger;
import org.uispec4j.Window;
import org.uispec4j.assertion.dependency.InternalAssert;
import org.uispec4j.utils.ComponentUtils;
import org.uispec4j.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * WindowHandler implementation dedicated to file chooser dialogs.<p>
 * Sample usage:
 * <pre><code>
 * WindowInterceptor
 *   .run(openMenu.triggerClick())
 *   .process(FileChooserHandler.init()
 *            .titleEquals("Select a template file")
 *            .assertAcceptsFilesOnly()
 *            .select("/home/bond/file.txt"))
 *   .run();
 * </code></pre>
 *
 * @see <a href="http://www.uispec4j.org/interception.html">Intercepting windows</a>
 */
public class FileChooserHandler {
  private DialogHandler handler = new DialogHandler();

  /**
   * Creates a new interceptor with the associated {@link Trigger}
   */
  public static FileChooserHandler init() {
    return new FileChooserHandler();
  }

  public FileChooserHandler assertIsOpenDialog() {
    handler.add(new DialogTypeFileChooserHandler(JFileChooser.OPEN_DIALOG));
    return this;
  }

  public FileChooserHandler assertIsSaveDialog() {
    handler.add(new DialogTypeFileChooserHandler(JFileChooser.SAVE_DIALOG));
    return this;
  }

  public FileChooserHandler assertCurrentDirEquals(final File currentDir) {
    handler.add(new FileChooserInternalHandler() {
      public void process(JFileChooser fileChooser) {
        InternalAssert.assertEquals("Unexpected current directory -",
                                    currentDir, fileChooser.getCurrentDirectory());
      }
    });
    return this;
  }

  public FileChooserHandler titleEquals(final String title) {
    handler.add(new FileChooserInternalHandler() {
      public void process(JFileChooser fileChooser) {
        InternalAssert.assertEquals("Unexpected title -",
                                    title, fileChooser.getDialogTitle());
      }
    });
    return this;
  }

  public FileChooserHandler assertApplyButtonTextEquals(final String text) {
    handler.add(new FileChooserInternalHandler() {
      public void process(JFileChooser fileChooser) {
        InternalAssert.assertEquals("Unexpected apply button text -",
                                    text, fileChooser.getApproveButtonText());
      }
    });
    return this;
  }

  public FileChooserHandler assertAcceptsFilesOnly() {
    handler.add(new FileSelectionModeFileChooserHandler(JFileChooser.FILES_ONLY));
    return this;
  }

  public FileChooserHandler assertAcceptsFilesAndDirectories() {
    handler.add(new FileSelectionModeFileChooserHandler(JFileChooser.FILES_AND_DIRECTORIES));
    return this;
  }

  public FileChooserHandler assertAcceptsDirectoriesOnly() {
    handler.add(new FileSelectionModeFileChooserHandler(JFileChooser.DIRECTORIES_ONLY));
    return this;
  }

  public FileChooserHandler assertMultiSelectionEnabled(final boolean enabled) {
    handler.add(new FileChooserInternalHandler() {
      public void process(JFileChooser fileChooser) {
        boolean actual = fileChooser.isMultiSelectionEnabled();
        if (actual != enabled) {
          InternalAssert.fail(actual ? "Multi selection is enabled." : "Multi selection is not enabled.");
        }
      }
    });
    return this;
  }

  public WindowHandler select(final File file) {
    handler.add(new FileChooserInternalHandler() {
      public void process(JFileChooser fileChooser) {
        fileChooser.setSelectedFile(file);
      }
    });
    return handler;
  }

  public WindowHandler select(final File[] files) {
    handler.add(new FileChooserInternalHandler() {
      public void process(JFileChooser fileChooser) {
        fileChooser.setSelectedFiles(files);
      }
    });
    return handler;
  }

  public WindowHandler select(String fileName) {
    return select(new File(fileName));
  }

  public WindowHandler select(String[] fileNames) {
    File[] files = new File[fileNames.length];
    for (int i = 0; i < files.length; i++) {
      files[i] = new File(fileNames[i]);
    }
    return select(files);
  }

  /**
   * Clicks on "Cancel".
   */
  public WindowHandler cancelSelection() {
    handler.add(new FileChooserInternalHandler() {
      public void process(JFileChooser fileChooser) {
        fileChooser.cancelSelection();
      }
    });
    return handler;
  }

  private static class DialogHandler extends WindowHandler {
    private List fileChooserHandlers = new ArrayList();

    public DialogHandler() {
      super("FileChooserHandler");
    }

    void add(FileChooserInternalHandler handler) {
      fileChooserHandlers.add(handler);
    }

    public Trigger process(final Window window) {
      Component[] components = window.getSwingComponents(JFileChooser.class);
      if (components.length != 1) {
        InternalAssert.fail("The shown window is not a file chooser - window content:" +
                            Utils.LINE_SEPARATOR + window.getDescription());
      }
      JFileChooser fileChooser = (JFileChooser)components[0];
      for (Iterator iterator = fileChooserHandlers.iterator(); iterator.hasNext();) {
        FileChooserInternalHandler handler = (FileChooserInternalHandler)iterator.next();
        handler.process(fileChooser);
      }
      fileChooser.approveSelection();
      return new Trigger() {
        public void run() throws Exception {
          ComponentUtils.close(window);
        }
      };
    }
  }

  private interface FileChooserInternalHandler {
    void process(JFileChooser fileChooser);
  }

  private static class DialogTypeFileChooserHandler implements FileChooserInternalHandler {
    int expectedType;

    public DialogTypeFileChooserHandler(int expectedType) {
      this.expectedType = expectedType;
    }

    public void process(JFileChooser fileChooser) {
      if (fileChooser.getDialogType() != expectedType) {
        InternalAssert.fail(getChooserType(fileChooser));
      }
    }

    private String getChooserType(JFileChooser fileChooser) {
      String message = "Chooser is in '";
      switch (fileChooser.getDialogType()) {
        case JFileChooser.OPEN_DIALOG:
          message += "open";
          break;
        case JFileChooser.SAVE_DIALOG:
          message += "save";
          break;
        default:
          message += "custom";
      }
      message += "' mode";
      return message;
    }
  }

  private static class FileSelectionModeFileChooserHandler implements FileChooserInternalHandler {
    int expectedMode;

    public FileSelectionModeFileChooserHandler(int expectedMode) {
      this.expectedMode = expectedMode;
    }

    public void process(JFileChooser fileChooser) {
      int actualMode = fileChooser.getFileSelectionMode();
      if (actualMode != expectedMode) {
        InternalAssert.fail(getMessage(actualMode));
      }
    }

    private String getMessage(int mode) {
      String message = "The file chooser accepts ";
      switch (mode) {
        case JFileChooser.FILES_ONLY:
          message += "files only.";
          break;
        case JFileChooser.DIRECTORIES_ONLY:
          message += "directories only.";
          break;
        case JFileChooser.FILES_AND_DIRECTORIES:
          message += "both files and directories.";
      }
      return message;
    }
  }
}