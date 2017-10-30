package com.vsu.smartvuetypef.view;

public class FaceView{

	public FaceListener listener;
	
	public FaceView(){
		this.listener = null;
	}
	public interface FaceListener{
		public void onLeftFace(int fps);
		
		public void onRightFace(int fps);
	}
	
	public void setFaceListener(FaceListener listener){
		this.listener = listener;
	}
	
	public void doSomethingToFace(){
		 if (listener != null)
			 
             listener.onRightFace(2); // <---- fire listener here
	}
}
