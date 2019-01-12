package mymidletpackage;
import java.io.*;
import java.util.*;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;

public class MapMidlet extends MIDlet implements CommandListener
{
    private String url = "http://localhost:8084/ProjectWebApplication/MobileServlet";
    private Display display;
    private Command exit = new Command("Exit", Command.EXIT, 1);
    private Command connect = new Command("Show Map", Command.SCREEN, 1);
    private Command instr = new Command("Instructions", Command.SCREEN, 1);
    private Command back = new Command("Back", Command.SCREEN, 1);
    private Form menu;
    private Form instructions;
    private ChoiceGroup start;
    private ChoiceGroup goal;
    private ChoiceGroup type;
    String action = "map";
    String subAction = "";
    String buildings;
    String startLocation = "";
    String goalLocation = "";
    String typeChoice = "";
    String width;
    String height;
    MapCanvas newMapCanvas = new MapCanvas(this);
                    
    public MapMidlet() throws Exception
    {
        display = Display.getDisplay(this);
    }
    
    public void startApp()
    {
        try
        {
            /** Request the building choices from the servlet for this session*/
            subAction = "buildings";
            HttpConnection c = (HttpConnection) Connector.open(url);
            
            c.setRequestProperty("User-Agent","Profile/MIDP-2.0, Configuration/CLDC-1.1");
            c.setRequestProperty("Content-Language","en-US");
            c.setRequestMethod(HttpConnection.POST);
                
            DataOutputStream os = c.openDataOutputStream();
            os.writeUTF(action.trim());
            os.writeUTF(subAction.trim());
            os.flush();
            os.close();
            
            /** Get the buildings response from the servlet *******************/
            DataInputStream is = c.openDataInputStream();
            int ch;
            StringBuffer sb = new StringBuffer();            
                                
            while ((ch = is.read()) != -1)
            {
                sb.append((char)ch);
            }
                
            buildings = sb.toString();
            is.close();
            c.close();
                        
            displayMenu();           
        }
        
        catch (Exception e)
        {
            e.printStackTrace();
        }        
    }
    
    public void displayMenu()
    {
        menu = new Form("Select a start and goal location");        
        start = new ChoiceGroup("Start:", ChoiceGroup.POPUP);
        goal = new ChoiceGroup("Goal:", ChoiceGroup.POPUP);
        String[] splitBuildings = split(buildings, ",");
        
        for (int x = 0; x < splitBuildings.length; x++)
        {
            start.append(splitBuildings[x], null);
            goal.append(splitBuildings[x], null);
        }
        
        type = new ChoiceGroup("Map Type:", ChoiceGroup.EXCLUSIVE);
        type.append("Fully Scaled", null);
        type.append("Large Scrollable", null);
        type.append("Sub-Section Zoom", null);
        
        String text = "\nFully Scaled = approx 41 KB";
        String text2 = "Large Scrollable = approx 205 KB";
        String text3 = "Sub-Section Zoom = approx between 5-175 KB";
        
        menu.append(start);
        menu.append(goal);
        menu.append(type);
        menu.append(text);
        menu.append(text2);
        menu.append(text3);
        menu.addCommand(exit);
        menu.addCommand(connect);
        menu.addCommand(instr);
        menu.setCommandListener(this);
        display.setCurrent(menu);
    }
    
    public void displayInstructions()
    {
        instructions = new Form("Instructions");        
        String text = "Use your device's menu button to access 'back' or 'exit' options on the map display screen\n";
        String text2 = "Fully Scaled: no user interaction\n";
        String text3 = "Large Scrollable: use the arrow keys to scroll the map\n";
        String text4 = "Sub-Section Zoom: use the arrow keys to scroll the map if the size is applicable";
        
        instructions.append(text);
        instructions.append(text2);
        instructions.append(text3);
        instructions.append(text4);
        instructions.addCommand(back);
        instructions.setCommandListener(this);
        display.setCurrent(instructions);
    }
    
    /**************************************************************************/
    
    public void postRequest()
    {
        try
        {
            /** Post the start and goal choices to the servlet ****************/
            subAction = "processRequest";           
            width = ""+newMapCanvas.getScreenWidth();
            height = ""+newMapCanvas.getScreenHeight();
                                    
            HttpConnection c = (HttpConnection) Connector.open(url);

            c.setRequestProperty("User-Agent","Profile/MIDP-2.0, Configuration/CLDC-1.1");
            c.setRequestProperty("Content-Language","en-US");
            c.setRequestMethod(HttpConnection.POST);

            DataOutputStream os = c.openDataOutputStream();
            os.writeUTF(action.trim());
            os.writeUTF(subAction.trim());
            os.writeUTF(startLocation.trim());
            os.writeUTF(goalLocation.trim());
            os.writeUTF(typeChoice.trim());
            os.writeUTF(width.trim());
            os.writeUTF(height.trim());            
            os.flush();
            os.close();
            
            /** Get the image response from the server ************************/
            int length = (int) c.getLength();
            DataInputStream is = c.openDataInputStream();
            int ch;
            
            /** Headers */
            System.out.println("!! " + c.getResponseCode());
            System.out.println("!! " + c.getResponseMessage());
            System.out.println("!! " + c.getType());
            System.out.println("!! " + c.getLength());
            
            byte[] data; 
                        
            if (length != -1)
            { 
                int total = 0;
                data = new byte[length];
    
                while (total < length)
                {
                    total += is.read(data,total,length - total);
                }
            }
            
            else
            {
                ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                   
                while((ch = is.read()) != -1)
                {
                    tmp.write(ch);
                }
    
                data = tmp.toByteArray();
            }
            
            Image map = Image.createImage(data,0,data.length-1);
            Image copy = Image.createImage(map.getWidth(), map.getHeight());
            Graphics g = copy.getGraphics();
            g.drawImage(map, 0, 0, g.TOP | g.LEFT);
            
            /** Send the image to the canvas for painting *********************/
            newMapCanvas.setImage(copy);
            
            if (typeChoice.equals("Large Scrollable") == true)
            {
                newMapCanvas.setLargeImage();
            }
            
            else if (typeChoice.equals("Sub-Section Zoom") == true)
            {
                newMapCanvas.setLargeImage();
            }           
            
            /** Display the canvas and close the connections ******************/
            display.setCurrent(newMapCanvas);
                   
            is.close();
            c.close();
        }
        
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        catch (Error e)
        {
            e.printStackTrace();
            showAlert("Unfortunately the map requested can not be displayed due to an out of memory exception" +
                    " - please restart the application and try again");
        }
    }
    
    /**************************************************************************/
    
    private String[] split(String original, String separator)
    {
        Vector nodes = new Vector();
            
        // Parse nodes into vector
        int index = original.indexOf(separator);
            
        while(index >= 0)
        {
            nodes.addElement(original.substring(0,index));
            original = original.substring(index + separator.length());
            index = original.indexOf(separator);
        }
            
        // Get the last node
        if (original.equals("") == false)
        {
            nodes.addElement(original);
        }           
 
        // Create splitted string array
        String[] result = new String[nodes.size()];
            
        if(nodes.size() > 0)
        {
            for (int loop = 0; loop < nodes.size(); loop++)
            {
                result[loop] = (String)nodes.elementAt(loop);
            }
        }    
            
        return result;      
    }
    
    /**************************************************************************/
    
    /** Display message on screen */
    private void showAlert(String msg)
    {
        Alert a = new Alert("");
        a.setString(msg);
        a.setTimeout(Alert.FOREVER);
        display.setCurrent(a);
    }
    
    /**************************************************************************/
    
    public void commandAction(Command command, Displayable screen)
    {
        if (command == exit)
        {
            destroyApp(false);
            notifyDestroyed();
        }
        
        else if (command == instr)
        {
            displayInstructions();
        }
        
        else if (command == back)
        {
            displayMenu();
        }
        
        else if (command == connect)
        {
            startLocation = start.getString(start.getSelectedIndex());
            goalLocation = goal.getString(goal.getSelectedIndex());
            typeChoice = type.getString(type.getSelectedIndex());
                                                            
            if (startLocation == null | goalLocation == null)
            {
                System.out.println("Default");
                startLocation = "Cookworthy";
                goalLocation = "Main Hall";
            }
            
            if (startLocation.equals(goalLocation))
            {
                showAlert("Start and goal locations must not be the same");
            }
            
            else
            {
                Alert alert = new Alert ("Please Wait", "Loading...", null, null);
                alert.setTimeout(6000); 
                Gauge indicator = new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
                alert.setIndicator(indicator);
                display.setCurrent(alert);                
                
                /** Start thread to post request and receive response */
                AlertThread t = new AlertThread();                    
                t.start();                           
            }            
        }
    }
    
    /**************************************************************************/
    
    public void pauseApp() {}    
    public void destroyApp(boolean unconditional) {}
    
    /**************************************************************************/
    
    class MapCanvas extends Canvas implements CommandListener
    {
        /** For all paint choices */        
        private Command exit;
        private Command back;
        private MapMidlet midlet;
        Image image;
        int canvasWidth;
        int canvasHeight;
        
        /** For large scrollable image */
        private int m_imageWidth;
        private int m_imageHeight;
                
        /** Values required for movement */
        private int m_x;
        private int m_y;
        private int m_dx = 0;
        private int m_dy = 0;
        private Image m_screenBuf;
        private Graphics m_bufferGraphics; 
        
        public MapCanvas(MapMidlet midlet)
        {
            this.midlet = midlet;
            this.setFullScreenMode(true);
            System.out.println("Canvas Size = " + this.getWidth() + " " + this.getHeight());
            canvasWidth = this.getWidth();
            canvasHeight = this.getHeight();
            exit = new Command("Exit", Command.EXIT, 1);
            back = new Command("Back", Command.EXIT, 1);
            addCommand(back);
            addCommand(exit);
            setCommandListener(this);
        }
        
        public void setLargeImage()
        {
            m_imageWidth = image.getWidth();
            m_imageHeight = image.getHeight();
            m_x = (canvasWidth - m_imageWidth)/2;
            m_y = (canvasHeight - m_imageHeight)/2;
                
            // Create a buffered screen
            m_screenBuf = Image.createImage(canvasWidth, canvasHeight);
            m_bufferGraphics = m_screenBuf.getGraphics();
            m_bufferGraphics.setColor(0,0,0);
            m_bufferGraphics.fillRect(0,0,canvasWidth,canvasHeight);      
        }

        public void paint(Graphics g)
        {
            if (typeChoice.equals("Fully Scaled") == true)
            {
                g.drawImage(image, 0, 0, Graphics.TOP | Graphics.LEFT);
            }
            
            else if (typeChoice.equals("Large Scrollable") == true)
            {
                m_bufferGraphics.drawImage(image, m_x, m_y, m_bufferGraphics.TOP | m_bufferGraphics.LEFT);
                g.drawImage(m_screenBuf, 0, 0, Graphics.TOP | Graphics.LEFT);          
            }
            
            else if (typeChoice.equals("Sub-Section Zoom") == true)
            {        
                m_bufferGraphics.drawImage(image, m_x, m_y, m_bufferGraphics.TOP | m_bufferGraphics.LEFT);
                g.drawImage(m_screenBuf, 0, 0, Graphics.TOP | Graphics.LEFT);
            }            
        } 
        
        public void keyRepeated(int keyCode)
        {
            if (hasRepeatEvents())
            {
                moveImage(getGameAction(keyCode));
            }
        }
   
        public void keyPressed(int keyCode)
        {
            moveImage(getGameAction(keyCode));
        }
   
        public void keyReleased(int keyCode)
        {            
        }
   
        public void moveImage(int gameAction)
        {
            switch(gameAction)
            {
                case UP:
                    if (m_y < 0)
                    {
                        m_y = m_y + 40;
                        
                        if (m_y > 0)
                        {
                            m_y = 0;
                        }                       
                    }
                    break;
                    
                case DOWN:
                    if (m_y > canvasHeight - m_imageHeight)
                    {
                        m_y = m_y - 40;
                        
                        if (m_y < canvasHeight - m_imageHeight)
                        {
                            m_y = canvasHeight - m_imageHeight;
                        }                
                    }
                    break;
                    
                case LEFT:
                    if (m_x < 0)
                    {
                        m_x = m_x + 40;
                        
                        if (m_x > 0)
                        {
                            m_x = 0;
                        }                 
                    }
                    break;
                    
                case RIGHT:
                    if (m_x > canvasWidth - m_imageWidth)
                    {
                        m_x = m_x - 40;
                        
                        if (m_x < canvasWidth - m_imageWidth)
                        {
                            m_x = canvasWidth - m_imageWidth;
                        }                  
                    }
                    break;               
            }
            
            repaint();
        }  
        
        public void commandAction(Command command, Displayable screen)
        {
            if (command == exit)
            {
                destroyApp(false);
                notifyDestroyed();
            }
            
            else if (command == back)
            {
                midlet.displayMenu();
            }
        }
        
        public void setImage(Image newImageIn)
        {
            image = newImageIn;
        }         
        
        public int getScreenWidth()
        {
            return canvasWidth;
        }
        
        public int getScreenHeight()
        {
            return canvasHeight;
        }  
    }
    
    /**************************************************************************/
    
    class AlertThread extends Thread
    {
        public AlertThread()
        {            
        }
    
        public void run()
        {
            try
            {
                postRequest();
            }

            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }  
}
