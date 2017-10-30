package com.vsu.smartvuetypef.util;

public class Face {
	long time = -1;
	int faceSize = -1;

	public Face() {
	}

	public Face(int size){
		faceSize = size;
		time = 0;
	}

	public void saveFace(int face){
		faceSize = face;
	}

	public void saveFace(int face, long systemTime) {
		faceSize = face;
		time = systemTime;
	}

	public int getFaceSize() {
		return faceSize;
	}

	public long getTime() {
		return time;
	}
}
