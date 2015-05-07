/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author fosterl
 */
public class LineEnclosureFactoryTest {
    public LineEnclosureFactoryTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void hexagonalPrismEnclosure() {
        LineEnclosureFactory factory = new LineEnclosureFactory( 6, 16.0 );
        double[] startingCoords;
        double[] endingCoords;
        
        startingCoords = new double[] {
            0,0,0
        };
        endingCoords = new double[] {
            1,0,1
        };
        factory.addEnclosure(startingCoords, endingCoords);
        
        startingCoords = new double[]{
            0, 0, 0
        };
        endingCoords = new double[]{
            -1, 0, 1
        };
        factory.addEnclosure(startingCoords, endingCoords);

        startingCoords = new double[] {
            2000.0, 2000.0, 1500.0
        };
        endingCoords = new double[] {
            2000.0, 2000.0, 2000.0
        };
        factory.addEnclosure(startingCoords, endingCoords);
        startingCoords = new double[] {
            200.0, 200.0, 200.0
        };
        endingCoords = new double[]{
            100.0, 200.0, 200.0
        };
        factory.addEnclosure(startingCoords, endingCoords);
    }
}
