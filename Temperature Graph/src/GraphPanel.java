


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.*;
import gnu.io.*;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;





public class GraphPanel extends JFrame implements SerialPortEventListener {

	private JPanel Graph;	//panel that holds the graph
	private JTextField TempDisplay;		//Says "Current Temperature"
	private JTextField Temp;	//Displays current temperature that updates every second
	private String Title;	//Title of graph
	private double CurrentTemp;	//Holds temperature in C or F
	private int currentIndex=0;	//current index of graph array
	private long WindowSize = 60000;	//window range, changes from 60s to 300s
	private JRadioButton sixty = new JRadioButton("60 seconds",true);
	private JRadioButton threehundred = new JRadioButton("300 seconds");
	private JRadioButton fahr = new JRadioButton("Fahrenheit");
	private JRadioButton celc = new JRadioButton("Celsius",true);
	private JCheckBox ledbutton = new JCheckBox("Toggle LED");
	private XYPlot plot;
	private CommPortIdentifier portId;	
	private CommPortIdentifier theportId;	//the port we will communicate with
	private Enumeration portList;	//list of ports
	private InputStream inputStream;	//input buffer
	private OutputStream outputStream;	//output buffer
	private SerialPort serialPort;
	private byte[] Buffer = new byte[400];	//byte buffer
	private int[] Temperature = new int[2000];	//temp array
	private ChartPanel chartPanel;
	private TimeSeriesCollection dataset;	//graph dataset
	private TimeSeries series;
	private String stringCheck,tempString;
	private boolean ledON = false;	//LED on
	private boolean inC = true;		//in celsius or fahrenheit?

	/*Constructor, takes in name of app and chart title*/
	public GraphPanel(String applicationTitle, String chartTitle) throws IOException {

		super(applicationTitle);
		Title = chartTitle;
		EstablishConnection();	//Establishes a Serial Connection to Arduino
		MakeChart();	//Creates Real-time Graph of Temperatures
		ButtonDisplay(); //Adds buttons to graph
		TempDisplay();
	}

	/* This method adds buttons to JFrame      */
	private void ButtonDisplay() {

		sixty.addMouseListener(new MouseClickHandler() );
		threehundred.addMouseListener(new MouseClickHandler() );
		fahr.addMouseListener(new MouseClickHandler() );
		celc.addMouseListener(new MouseClickHandler() );
		ledbutton.addMouseListener(new MouseClickHandler());

		JPanel buttons = new JPanel();
		JPanel toggle = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.add(sixty,BorderLayout.NORTH);
		buttons.add(threehundred,BorderLayout.NORTH);			//adds button to button panel
		toggle.add(ledbutton);
		add(toggle,BorderLayout.SOUTH);
		add(buttons,BorderLayout.NORTH);

	}

	/* This method is used when the size of the graph changes. Adds and removes it quickly	*/
	
	public void RepaintChart(){

		this.remove(chartPanel);
		this.add(chartPanel);
	}

	/* Initializes the graph	*/
	private void MakeChart() {

		dataset = new TimeSeriesCollection();
		series = new TimeSeries("Temperature",Second.class);
		JFreeChart chart = createChart(dataset, Title); 	// based on the dataset we create the chart
		chartPanel = new ChartPanel(chart); // Put the chart into a panel
		//chartPanel.setMouseWheelEnabled(true);
		chartPanel.setMouseZoomable(false);
		//chartPanel.setPreferredSize(new java.awt.Dimension(800, 670));

		Graph = new JPanel(); // add it to our application
		Graph.add(chartPanel);
		chart.removeLegend();	//get rid of legend

		add(Graph,BorderLayout.CENTER);


	}


	/* This method establishes a serial connection with the Arduino	*/
	
	private void EstablishConnection() {

		portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {				//cycle through all ports
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				//System.out.println(portId.getName());
				theportId = portId;
				break;	//breaks out of while loop
			} 
		}

		try {
			serialPort = (SerialPort) theportId.open("SimpleReadApp", 2000);		//trys to open port


			inputStream = serialPort.getInputStream();	//input data
			outputStream = serialPort.getOutputStream();	//output data


			serialPort.addEventListener(this);	//event listener
			serialPort.notifyOnDataAvailable(true);	//notifies port that data is available

			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			serialPort.enableReceiveTimeout(500);

		}//end try statement

		catch (PortInUseException e) {
			System.out.println("PORT IS IN USE ALREADY");
		}
		catch (NullPointerException n) {
			System.out.println("NO DEVICE CONNECTED");
		}

		catch(TooManyListenersException e) {
			System.out.println("Too many Listeners");
		}

		catch(UnsupportedCommOperationException e) {
			System.out.println("Unsupported comm operation");
		}
		catch (IOException e) {
			System.out.println("IOException");
		}



	}


	/* Creates chart and plot specifics	*/
	private JFreeChart createChart(XYDataset dataset, String title) {

		JFreeChart chart = ChartFactory.createTimeSeriesChart(title,          // chart title
				"Time",                // data
				"Temperature (Celsius)",                   // include legend
				dataset,
				true,
				true,
				false);

		plot = (XYPlot) chart.getXYPlot();
		plot.setBackgroundPaint(Color.LIGHT_GRAY);
		plot.setDomainGridlinePaint(Color.WHITE);
		plot.getDomainAxis().setFixedAutoRange(WindowSize);				//WindowSize is changed through the buttons, 60 or 300

		plot.setRangeGridlinePaint(Color.WHITE);
		plot.setRangeAxisLocation(AxisLocation.TOP_OR_RIGHT);       //moves y-axis to the right side of screen
		plot.getRangeAxis().setRange(10, 50);                       //graph displays 10-50


		final ValueAxis axis = (ValueAxis) plot.getDomainAxis();

		return chart;

	}

	/* This method will handle the temperature display		*/
	private void TempDisplay(){

		JPanel tdisplay = new JPanel();
		tdisplay.setLayout( new GridLayout(8,1,5,5));
		Temp = new JTextField(Double.toString(CurrentTemp));
		Temp.setEditable(false);
		Temp.setFont(new Font("Serif", Font.BOLD, 20));
		Temp.setHorizontalAlignment(JTextField.CENTER);
		Temp.setBorder(javax.swing.BorderFactory.createEmptyBorder());

		TempDisplay = new JTextField("Current Temperature",20);
		TempDisplay.setEditable(false);
		TempDisplay.setFont(new Font("Serif", Font.BOLD, 14));
		TempDisplay.setHorizontalAlignment(JTextField.CENTER);
		TempDisplay.setBorder(javax.swing.BorderFactory.createEmptyBorder());
		JPanel j = new JPanel();
		j.add(celc);
		j.add(fahr);

		tdisplay.add(TempDisplay);
		tdisplay.add(Temp);
		tdisplay.add(j);
		add(tdisplay, BorderLayout.EAST);

	}

	/* This handles the events when the buttons are clicked. Also sends the data to the Arduino as well to turn LED ON or OFF	*/
	private class MouseClickHandler extends MouseAdapter {

		public void mouseClicked(MouseEvent event) {
			if (event.getSource() == sixty) {
				plot.getDomainAxis().setFixedAutoRange(60000); 			//changes window size to 60
				threehundred.setSelected(false);
				sixty.setSelected(true);
			}

			if (event.getSource() == threehundred) {
				plot.getDomainAxis().setFixedAutoRange(300000);			//changes window size to 300s
				sixty.setSelected(false);
				threehundred.setSelected(true);
			}

			if (event.getSource() == fahr) {
				celc.setSelected(false);
				CurrentTemp = ctof(CurrentTemp);
				inC=false;
				fahr.setSelected(true);
				celc.setSelected(false);

			}

			if (event.getSource() == celc) {
				fahr.setSelected(false);
				CurrentTemp = ftoc(CurrentTemp);
				inC=true;
				celc.setSelected(true);
				fahr.setSelected(false);
			}

			if (event.getSource() == ledbutton) {
				byte[] b = new byte[1];
				try {
					if (ledON){
						b[0] = (byte) 0xAA;
						outputStream.write(b, 0, b.length);		//if it's on, turn it off
						ledON = !ledON;
					}
					else{
						b[0] = (byte) 0xBB;
						outputStream.write(b,0,b.length);		//if it's off, turn it on
						ledON = !ledON;
					}
				} catch (IOException e) {
					System.out.println("IO EXCEPTION");
				}
				  catch (NullPointerException n) {
					System.out.println("NO LED CONNECTED");
				}
			}
		}

	}

	/*	Method handles serial events. Sends incoming temperatures to the graph, and Temperature array. Also sends into to the display of the temperature.	*/
	@Override
	public void serialEvent(SerialPortEvent e) {
		if (e.getEventType() == SerialPortEvent.DATA_AVAILABLE){
			int scount=0;	//counts number of s's in string
			int fcount=0;	//counts number of f's in string

			try {
				int availablebytes = inputStream.available();
				if(availablebytes > 0){
					inputStream.read(Buffer,0,availablebytes);
					stringCheck = new String(Buffer,0,availablebytes);

					for (int i=0; i<stringCheck.length(); i++){
						if (stringCheck.charAt(i) == 's'){
							scount++;
						}
						if (stringCheck.charAt(i) == 'f'){
							fcount++;
						}
					}

					if (stringCheck.startsWith("s") && stringCheck.endsWith("f") && scount==1 && fcount==1 ){		//checks that whole number is sent, not in pieces
						tempString = stringCheck.substring(1, stringCheck.indexOf("f"));
						Temperature[currentIndex] = Integer.parseInt(tempString);

					}
					else {
						Temperature[currentIndex] = Temperature[currentIndex-1];			
					}

					if (inC) {	//if celsius button is selected
						Temp.setText(tempString);
					}
					else {
						double d = Double.parseDouble(tempString);
						d = ctof(d);
						Temp.setText(Double.toString(d));
						
					}
					addData(Temperature[currentIndex]);
					currentIndex++;
					Thread.sleep(1000);	//data is read every second

				}

			} catch (IOException e1) {
				System.out.println("DATA NOT AVAILABLE");
				serialPort.close();
			}
			catch (InterruptedException f) {
				f.printStackTrace();
			}

		}
	}
	/*	This method converts fahrenheit to celsius		*/
	public static double ftoc(double t) {
		t = ((t-32.0) * (5.0/9.0));
		return t;

	}

	/*	This method converts celsius to fahrenheit		*/
	public static double ctof(double t) {
		t = (t*(9.0/5.0)+32.0);
		return t;

	}
	/*	Adds data to the graph	*/
	private void addData(int i) {

		series.add(new Second(), i);
		dataset.addSeries(series);
	}



} 
