package org.janelia.horta.camera;

import org.janelia.horta.camera.InterpolatorKernel;
import org.janelia.geometry3d.Quaternion;

/**
 * Interpolates Java primitive types, plus Quaternions
 * @author brunsc
 */
public class PrimitiveInterpolator implements Interpolator<Quaternion>
{
    private final InterpolatorKernel kernel;
    
    public PrimitiveInterpolator(InterpolatorKernel kernel) {
        this.kernel = kernel;
    }
    
    // Assumes points are spaced equally
    public double interpolate_equidistant(double t, double p0, double p1, double p2, double p3)
    {
        return kernel.interpolate_equidistant(t, p0, p1, p2, p3);
    }
    
    // General case does not require points to be spaced equally
    public double interpolate(double t, // t in range [0-1], between points p1 and p2
            double p0, double p1, double p2, double p3, // values at 4 points
            double t0, double t1, double t2, double t3) // distribution of 4 points along x axis
    {
        // Logger logger = LoggerFactory.getLogger(this.class);
        
        double result;

        // Scale values to make them as-if equidistant
        double p0s = p0;
        double p3s = p3;
        if (t1 == t2) { // no need to interpolate if interval is empty
            result = (p1 + p2)/2.0;
        }
        else {
            if (t0 != t1) { // avoid giving infinity weight to duplicated points
                double dp = p1 - p0;
                dp *= (t2-t1)/(t1-t0);
                p0s = p1 - dp;
            }
            if (t2 != t3) {
                // p3s *= (t2-t1)/(t3-t2);
                double dp = p3 - p2;
                dp *= (t2-t1)/(t3-t2);
                p3s = p2 + dp;            }
            result = interpolate_equidistant(t, p0s, p1, p2, p3s);
        }
        // logger.info("Interpolating "+t+" ["+p0s+"("+p0+"),"+p1+","+p2+","+p3s+"("+p3+")] to "+result);
        return result;
    }
    
    // Specialization for floats
    public float interpolate_equidistant(double t, float p0, float p1, float p2, float p3)
    {
        return (float) interpolate_equidistant(t, (double)p0, (double)p1, (double)p2, (double)p3);
    }
    public float interpolate(double t, 
            float p0, float p1, float p2, float p3,
            double t0, double t1, double t2, double t3) 
    {
        return (float)interpolate(t, 
                (double)p0, (double)p1, (double)p2, (double)p3,
                t0, t1, t2, t3);
    }
    
    // Specialization for integers
    public int interpolate_equidistant(double t, int p0, int p1, int p2, int p3)
    {
        return (int)Math.round(interpolate_equidistant(t, (double)p0, (double)p1, (double)p2, (double)p3));
    }
    public int interpolate(double t, 
            int p0, int p1, int p2, int p3,
            double t0, double t1, double t2, double t3) 
    {
        return (int)Math.round(interpolate(t, 
                (double)p0, (double)p1, (double)p2, (double)p3,
                t0, t1, t2, t3));
    }
    
    // Specialization for boolean values
    public boolean interpolate_equidistant(double t, boolean p0, boolean p1, boolean p2, boolean p3)
    {
        double d0 = p0 ? 1.0 : 0.0;
        double d1 = p1 ? 1.0 : 0.0;
        double d2 = p2 ? 1.0 : 0.0;
        double d3 = p3 ? 1.0 : 0.0;
        double result = interpolate_equidistant(t, d0, d1, d2, d3);
        return result >= 0.5;
    }
    public boolean interpolate(double t, 
            boolean p0, boolean p1, boolean p2, boolean p3,
            double t0, double t1, double t2, double t3) 
    {
        double d0 = p0 ? 1.0 : 0.0;
        double d1 = p1 ? 1.0 : 0.0;
        double d2 = p2 ? 1.0 : 0.0;
        double d3 = p3 ? 1.0 : 0.0;
        double result = interpolate(t, d0, d1, d2, d3, t0, t1, t2, t3);
        return result >= 0.5;
    }
    
    // specialization for Quaterions
    // Translated from page 449 of "Visualizing Quaternions" by Andrew J. Hanson.
    // TODO: I have no idea how to do a non-uniform version of Quaternion interpolation.
    @Override
    public Quaternion interpolate_equidistant(double t, Quaternion q0, Quaternion q1, Quaternion q2, Quaternion q3)
    {
        return kernel.interpolate_equidistant(t, q0, q1, q2, q3);
    }

    @Override
    public Quaternion interpolate(
            double ofTheWay, 
            Quaternion p0, Quaternion p1, Quaternion p2, Quaternion p3, 
            double t0, double t1, double t2, double t3) 
    {
        // TODO: I have no idea how to do a non-uniform version of Quaternion interpolation.
        return interpolate_equidistant(ofTheWay, p0, p1, p2, p3);
    }
    
}
