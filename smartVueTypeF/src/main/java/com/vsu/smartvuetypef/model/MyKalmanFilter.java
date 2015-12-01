package main.java.com.vsu.smartvuetypef.model;

import org.opencv.core.Core;
import org.opencv.core.Mat;

public class MyKalmanFilter {
	public Mat statePre, statePost, transitionMatrix, processNoiseCov,
	measurementMatrix, measurementNoiseCov, errorCovPre, errorCovPost,
	gain, controlMatrix, temp1, temp2, temp3, temp4, temp5, temp6;
	
	//Dimensionality of state, measurement, and control vector 
	public MyKalmanFilter(int dynamParams, int measureParams, int controlParams, int type)
	{
	    init(dynamParams, measureParams, controlParams, type);
	}

	public void init(int DP, int MP, int CP, int type)
	{
	    //Core.CV_Assert( DP > 0 && MP > 0 );
	    //CV_Assert( type == CV_32F || type == CV_64F );
	    CP = Math.max(CP, 0);
	    
	    //predicted state (x'(k)): x(k)=A*x(k-1)+B*u(k) 
	    statePre =Mat.zeros(DP, 1, type);
	    //corrected state (x(k)): x(k)=x'(k)+K(k)*(z(k)-H*x'(k))
	    statePost = Mat.zeros(DP, 1, type);
	    //state transition matrix (A)
	    transitionMatrix = Mat.eye(DP, DP, type);

	    //process noise covariance matrix (Q)
	    processNoiseCov = Mat.eye(DP, DP, type);
 	    //measurement matrix (H)
	    measurementMatrix = Mat.zeros(MP, DP, type);
	    //process noise covariance matrix (Q)
	    measurementNoiseCov = Mat.eye(MP, MP, type);

	    //priori error estimate covariance matrix (P'(k)): P'(k)=A*P(k-1)*At + Q)*/
	    errorCovPre = Mat.zeros(DP, DP, type);
	    //posteriori error estimate covariance matrix (P(k)): P(k)=(I-K(k)*H)*P'(k)
	    errorCovPost = Mat.zeros(DP, DP, type);
	    //Kalman gain matrix (K(k)): K(k)=P'(k)*Ht*inv(H*P'(k)*Ht+R) 
	    gain = Mat.zeros(DP, MP, type);

	    if( CP > 0 )
	        controlMatrix = Mat.zeros(DP, CP, type);
	    else
	        controlMatrix.release();

	    temp1 = new Mat();
	    temp2 = new Mat();
	    temp3 = new Mat();
	    temp4 = new Mat();
	    temp5 = new Mat();
	    temp6 = new Mat();
	    
	    temp1.create(DP, DP, type);
	    temp2.create(MP, DP, type);
	    temp3.create(MP, MP, type);
	    temp4.create(MP, DP, type);
	    temp5.create(MP, 1, type);
	    
	    //My Temps
	    temp6.create(DP,MP,type);
	    
	}

	public Mat predict()
	{
	    // update the state: x'(k) = A*x(k)
	    //statePre = transitionMatrix*statePost;
	    Core.gemm(transitionMatrix, statePost, 1, new Mat(), 0, statePre, 0);

	    // update error covariance matrices: temp1 = A*P(k)
	    //temp1 = transitionMatrix*errorCovPost;
	    Core.multiply(transitionMatrix, errorCovPost, temp1);

	    // P'(k) = temp1*At + Q
	    Core.gemm(temp1, transitionMatrix, 1, processNoiseCov, 1, errorCovPre, Core.GEMM_2_T);

	    // handle the case when there will be measurement before the next predict.
	    statePre.copyTo(statePost);
	    errorCovPre.copyTo(errorCovPost);

	    return statePre;
	}
	
	public Mat predict(Mat control)
	{
	    // update the state: x'(k) = A*x(k)
	    //statePre = transitionMatrix*statePost;
	    Core.gemm(transitionMatrix, statePost, 1, new Mat(), 0, statePre, 0);

	    if( !control.empty() ){
	        // x'(k) = x'(k) + B*u(k)
	        //statePre += controlMatrix*control;
	    	Core.multiply(controlMatrix, control, temp1);
	    	Core.add(statePre, temp1, statePre);
	    }

	    // update error covariance matrices: temp1 = A*P(k)
	    //temp1 = transitionMatrix*errorCovPost;
	    Core.multiply(transitionMatrix, errorCovPost, temp1);

	    // P'(k) = temp1*At + Q
	    Core.gemm(temp1, transitionMatrix, 1, processNoiseCov, 1, errorCovPre, Core.GEMM_2_T);

	    // handle the case when there will be measurement before the next predict.
	    statePre.copyTo(statePost);
	    errorCovPre.copyTo(errorCovPost);

	    return statePre;
	}

	public Mat correct(Mat measurement)
	{
	    // temp2 = H*P'(k)
	    //temp2 = measurementMatrix * errorCovPre;
		//Core.multiply(errorCovPre,measurementMatrix, temp2);
		Core.gemm(measurementMatrix, errorCovPre, 1, new Mat(), 0, temp2);
		

	    // temp3 = temp2*Ht + R
	    Core.gemm(temp2, measurementMatrix, 1, measurementNoiseCov, 1, temp3, Core.GEMM_2_T);

	    // temp4 = inv(temp3)*temp2 = Kt(k)
	    Core.solve(temp3, temp2, temp4, Core.DECOMP_SVD);

	    // K(k)
	    gain = temp4.t();

	    // temp5 = z(k) - H*x'(k)
	    //temp5 = measurement - measurementMatrix*statePre;
	    Core.gemm(measurementMatrix, statePre, 1, new Mat(), 0, temp4);
	    Core.subtract(measurement, temp4, temp5);

	    // x(k) = x'(k) + K(k)*temp5
	    
	    //statePost = statePre + gain*temp5;
	    Core.gemm(gain, temp5, 1, new Mat(), 0, temp6);
	    Core.add(statePre, temp6, statePost);

	    // P(k) = P'(k) - K(k)*temp2
	    //errorCovPost = errorCovPre - gain*temp2;
	    Core.gemm(gain, temp2, 1, new Mat(), 0, temp6);
	    Core.subtract(errorCovPre, temp6, errorCovPost);

	    return statePost;
	}
}
