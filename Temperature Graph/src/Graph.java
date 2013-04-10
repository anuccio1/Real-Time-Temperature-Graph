import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.io.IOException;

import javax.swing.JFrame;

import org.jfree.ui.RefineryUtilities;

public class Graph {
	
	/*	The main method creats a GraphPanel object, and initiates the Graph		*/
   public static void main(String[] args) throws IOException {
          final GraphPanel demo = new GraphPanel("Lab Two", "Temperature vs. Time");
          demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          
          demo.getContentPane().addHierarchyBoundsListener(new HierarchyBoundsListener(){
        	  
              @Override
              public void ancestorMoved(HierarchyEvent e) {}
              @Override
              public void ancestorResized(HierarchyEvent e) {
            	  
            	  demo.RepaintChart();
            	  
              }           
          });
          
          demo.pack();
          RefineryUtilities.centerFrameOnScreen(demo);
          demo.setVisible(true);

      }
} 