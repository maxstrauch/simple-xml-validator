
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Simple GUI-based XML against DTD/XSD validator.
 * 
 * @author maximilianstrauch
 */
public class XMLValidator {
    
    static boolean isInvalid;
    
    static ErrorHandler globalHandler = new ErrorHandler() {

        @Override
        public void error(SAXParseException e) throws SAXException {
            warn("Error at line %d, character %d: <code>%s</code>",
                    e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            isInvalid = true;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            error(e);
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            warn("Warning at line %d, character %d: <code>%s</code>",
                    e.getLineNumber(), e.getColumnNumber(), e.getMessage());
        }
    };
    
    public static void validateDTD(String xml, final String dtd) 
            throws Exception {
        
        info("Validating DTD for '%s'", xml);
        
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setValidating(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {
            
            @Override
            public InputSource resolveEntity(String publicId, String systemId) 
                    throws SAXException, IOException {
                info("Resolve DTD '%s' to '%s'", systemId, dtd);
                return new InputSource(new FileInputStream(dtd));
            }
        });
        builder.setErrorHandler(globalHandler);
        Document doc = builder.parse(xml);
        if (!isInvalid) {
            log("<h1 id=\"val\">VALID.</h1>");
        }
    }
    
    public static void validateXSD(String xml, String xsd) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setAttribute(
                "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                "http://www.w3.org/2001/XMLSchema"
        );
        factory.setAttribute(
                "http://java.sun.com/xml/jaxp/properties/schemaSource",
                xsd
        );
        DocumentBuilder parser = factory.newDocumentBuilder();
        parser.setErrorHandler(globalHandler);
        Document doc = parser.parse(xml);
        if (!isInvalid) {
            log("<h1 id=\"val\">VALID.</h1>");
        }
    }
    
    // -------------------------------------------------------------------------
    // All that GUI stuff
    // -------------------------------------------------------------------------
    
    static JEditorPane logging;
    static StringBuilder textBuffer = new StringBuilder();
    
    static void clear() {
        textBuffer.setLength(0);
        apply();
    }
    
    static void info(String str, Object...args) {
        textBuffer.append("<p class=\"info\">" + String.format(str, args) + "</p>");
        apply();
    }
    
    static void warn(String str, Object...args) {
        textBuffer.append("<p class=\"err\">" + String.format(str, args) + "</p>");
        apply();
    }
    
    static void log(String str, Object...args) {
        textBuffer.append(String.format(str, args));
        apply();
    }
    
    static void apply() {
        String str = "";
        logging.setText(
                str = "<html><head>" +
                    "<style type=\"text/css\">" + 
                        "h1, h2 {font-family: Arial, sans-serif;}" + 
                        "#val {color:green;background-color:#E5FFE5;}" +
                        "#inval {color:red;background-color:#FFE5E5;}" +
                        "#val, #inval {margin:5px;padding:7px;}" +
                        ".info, .err {margin:5px;padding:7px;}" +
                        ".info {font-family: Arial, sans-serif;background-color:#eeeeee}" + 
                        ".err {font-family: Arial, sans-serif;color:red;background-color:#FFE5E5}" + 
                    "</style>" +
                "</head><body>" + textBuffer.toString() + "</body></html>"
        );
    }
    
    static void createAndShowGUI() {
        JFrame frame = new JFrame("XSD & DTD Validator");
        frame.setContentPane(new JPanel());
        ((JPanel) frame.getContentPane()).setBorder(new EmptyBorder(7, 7, 7, 7));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(7, 7));
        
        final JTextField xmlDoc = new JTextField();
        final JTextField xmlSchema = new JTextField();
        
        JButton openDoc = new JButton(new AbstractAction("Open XML ...") {

            @Override
            public void actionPerformed(ActionEvent e) {
                File f = selectFile((JButton) e.getSource(), "XML document", "xml");
                xmlDoc.setText(f == null ? "" : f.getAbsolutePath());
            }
        });
        
        JButton openSchema = new JButton(new AbstractAction("Open DTD/XSD ...") {

            @Override
            public void actionPerformed(ActionEvent e) {
                File f = selectFile((JButton) e.getSource(), 
                        "DTD or XSD definition file", "dtd", "xsd");
                xmlSchema.setText(f == null ? "" : f.getAbsolutePath());
            }
        });
        
        JButton validate = new JButton(new AbstractAction("Validate now!") {

            @Override
            public void actionPerformed(ActionEvent e) {
                clear();
                isInvalid = false;
                
                File doc = new File(xmlDoc.getText());
                File sch = new File(xmlSchema.getText());
                
                if (!doc.exists() || !doc.canRead() || doc.isDirectory() ||
                        !sch.exists() || !sch.canRead() || sch.isDirectory()) {
                    warn("Error: one of the given files is invalid!");
                    warn("Stop.");
                    return;
                }
                
                try {
                    if (sch.getName().endsWith(".dtd")) {
                        validateDTD(doc.getAbsolutePath(), sch.getAbsolutePath());
                    } else {
                        validateXSD(doc.getAbsolutePath(), sch.getAbsolutePath());
                    }
                    
                    if (isInvalid) {
                        log("<h1 id=\"inval\">INVALID.</h1>");
                    }
                } catch (Exception ex) {
                    log("<h1 id=\"inval\">INVALID.</h1>");
                    warn("Program exception caught during validation:\n");
                    String exStr = exceptionToString(ex);
                    warn("<code>" + (exStr == null ? "N/A" : exStr.trim()) + "</code>");
                    warn("Stop.");
                }
                
                logging.setCaretPosition(0);
            }
        });
        
        JPanel container = new JPanel(new GridLayout(3, 1, 4, 4));
        {
            Box hbox = Box.createHorizontalBox();
            hbox.add(xmlDoc);
            hbox.add(openDoc);
            container.add(hbox);
        }
        {
            Box hbox = Box.createHorizontalBox();
            hbox.add(xmlSchema);
            hbox.add(openSchema);
            container.add(hbox);
        }
        
        container.add(validate);
        frame.add(container, BorderLayout.NORTH);
        
        logging = new JEditorPane("text/html", "");
        log("<h1>Ready.</h1><h2>Select files or drop them onto this window "
                + "and hit \"Validate now!\" to start.</h2>");
        logging.setEditable(false);
        JScrollPane sp = new JScrollPane(logging);
        sp.setPreferredSize(new Dimension(800, 400));
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(sp, BorderLayout.CENTER);
        frame.add(new JLabel("Maximilian Strauch. 2015.", SwingConstants.RIGHT), BorderLayout.SOUTH);
        
        JPanel glassPane = new JPanel();
        glassPane.setOpaque(false);
        frame.setGlassPane(glassPane);
        frame.getGlassPane().setVisible(true);
        
        // DnD
        DropTarget dt = new DropTarget(
            glassPane,
            DnDConstants.ACTION_COPY_OR_MOVE,
            new DropTargetAdapter() {

                @Override
                public void drop(DropTargetDropEvent evt) {
                    if (evt.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        evt.acceptDrop(DnDConstants.ACTION_COPY);
                        Transferable tr= evt.getTransferable();
                        try {
                            List<File> files = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                            
                            for (int i = 0; i < files.size(); i++) {
                                String name = files.get(i).getName().toLowerCase();
                                if (name.endsWith(".dtd") || name.endsWith(".xsd")) {
                                    xmlSchema.setText(files.get(i).getAbsolutePath());
                                }
                                if (name.endsWith(".xml")) {
                                    xmlDoc.setText(files.get(i).getAbsolutePath());
                                }
                            }
                        } catch (Exception ex) {
                            /* Silence is gold */
                        }
                    }
                }
            },
            true
        );
        
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
    }
    
    private static File selectFile(Component parent, final String name, 
            final String...suffixes) {
        JFileChooser fc = new JFileChooser(new File(".")); // Lazy
        fc.setMultiSelectionEnabled(false);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                boolean extension = false;
                if (f.getName().contains(".")) {
                    String e = f.getName()
                            .substring(f.getName().indexOf('.') + 1)
                            .toLowerCase();
                    for (String tmp : suffixes) {
                        tmp = tmp.toLowerCase();
                        if ("*".equals(tmp) || tmp.equals(e)) {
                            extension = true;
                            break;
                        }
                    }
                }
                return f.isDirectory() || extension;
            }

            @Override
            public String getDescription() {
                return name;
            }
        });
        int retrn = fc.showOpenDialog(parent);
        if (retrn == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                return file.getCanonicalFile();
            } catch (IOException e) {
                return file;
            }
        }
        return null;
    }
    
    
    /**
     * Prints the stack trace of a {@link Throwable} to a {@link String} and
     * returns the value.
     * 
     * @param t the {@link Throwable} to print the stack trace of.
     * @return the stack trace as {@link String}.
     */
    public static final String exceptionToString(Throwable t) {
        if (t == null) {
            return new String();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        t.printStackTrace(pw);
        pw.flush();
        pw.close();
        return new String(baos.toByteArray());
    }
    
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }
    
}
