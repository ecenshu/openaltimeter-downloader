/*
    openaltimeter -- an open-source altimeter for RC aircraft
    Copyright (C) 2010  Jony Hudson, Jan Steidl
    http://openaltimeter.org

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openaltimeter.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Vector;

public class FlightLog {
 
	public ArrayList<LogEntry> logData = new ArrayList<LogEntry>();
	public ArrayList<Annotation> annotations = new ArrayList<Annotation>();
	public double logInterval = 0.5;
	
	private static final int BASE_PRESSURE_SAMPLES = 20;
	private static final long PRESSURE_EMPTY_DATA = -1;
	
	public void add(LogEntry entry) {
		logData.add(entry);
	}
	
	// method for calculating base pressure for data starting at startIndex
	public double calculateBasePressure(int startIndex)
	{
		int count = 0;
		double basePressure = 0;
		for (int i = startIndex; (i < startIndex + BASE_PRESSURE_SAMPLES) && (i < logData.size()); i++)
		{
			LogEntry le = logData.get(i);
			if (le.pressure != PRESSURE_EMPTY_DATA) {
				count++;
				basePressure += le.pressure;
			}
			else
				break;
		}
		
		return basePressure / count;
	}
	
	// this method takes the raw log data and converts it into altitude data
	public void calculateAltitudes()
	{
		double basePressure = 0;
		
		for (int i = 0; i < logData.size(); i++)
		{
			LogEntry le = logData.get(i);
			
			if (le.pressure != PRESSURE_EMPTY_DATA) {
				if (basePressure == 0) 
					basePressure = calculateBasePressure(i);
				
				le.altitudeM = AltitudeConverter.altitudeMFromPressure(le.pressure, basePressure);
				le.altitudeFt = AltitudeConverter.feetFromM(le.altitudeM);
			}
			else 
				basePressure = 0;
		}
		// TEMP: add some annotations to test the serializer
		RegionAnnotation ra = new RegionAnnotation();
		ra.endTime = 10;
		ra.startTime = 5;
		ra.text = "Hello, region.";
		PointAnnotation pa = new PointAnnotation();
		pa.text = "Hello, point.";
		pa.time = 3;
		annotations.add(ra);
		annotations.add(pa);
	}

	public double[] getAltitudeFt() {
		int numPoints = logData.size();
		double[] data = new double[numPoints];
		for (int i = 0; i < numPoints; i++) data[i] = logData.get(i).pressure != PRESSURE_EMPTY_DATA ? logData.get(i).altitudeFt : 0;
		return data;
	}
	
	public double[] getAltitudeM() {
		int numPoints = logData.size();
		double[] data = new double[numPoints];
		for (int i = 0; i < numPoints; i++) data[i] = logData.get(i).pressure != PRESSURE_EMPTY_DATA ? logData.get(i).altitudeM : 0;
		return data;
	}
	
	public double[] getBattery()
	{
		//	mycarda 28 September 2011
		//	when an end-of-file is encountered, return the last valid battery reading rather than zero
		int numPoints = logData.size();
		double[] data = new double[numPoints];
		// just in case there is only one data point and that is PRESSURE_EMPTY_DATA (in which case 0.0 is a reasonable value)
		double lastVoltage = 0.0; 
		for (int i = 0; i < numPoints; i++) 
		{
			if (logData.get(i).pressure != PRESSURE_EMPTY_DATA)
			{
				data[i] = logData.get(i).battery;
				lastVoltage = data[i];
			}
			else
			{
				//	if there is no real data, use the last good value
				data[i] = lastVoltage;
			}
		}
		return data;
	}

	public double[] getTemperature() {
		//	mycarda 28 September 2011
		//	when an end-of-file is encountered, return the last valid temperature reading rather than zero
		int numPoints = logData.size();
		double[] data = new double[numPoints];
		
		// just in case there is only one data point and that is PRESSURE_EMPTY_DATA (in which case 0.0 is a reasonable value)
		double lastTemperature = 0.0; 
		for (int i = 0; i < numPoints; i++) 
		{
			if (logData.get(i).pressure != PRESSURE_EMPTY_DATA)
			{
				data[i] = logData.get(i).temperature;
				lastTemperature = data[i];
			}
			else
			{
				//	if there is no real data, use the last good value
				data[i] = lastTemperature;
			}
		}
		
		return data;
	}

	public double[] getServo()
	{
		int numPoints = logData.size();
		double[] data = new double[numPoints];
		for (int i = 0; i < numPoints; i++) data[i] = logData.get(i).pressure != PRESSURE_EMPTY_DATA ? logData.get(i).servo : 0;
		return data;
	}
	
	//	mycarda 29 September 2011
	//	show/hide end of file markers
	//	getEOF - find all end-of-file markers and add to data[]
	public double[] getEOF() {
		int numPoints = logData.size();
		double[] data = new double[numPoints];	// technically this is an unrealistic value however it is an upper limit
		int j = 0;
		for (int i = 0; i < numPoints; i++) 
		{
			if (logData.get(i).pressure == PRESSURE_EMPTY_DATA)
			{
				data[j] = i;	//store the index of the logData (corresponds to x-axis value)
				j++;
			}
		}
		
		return data;
	}
	
	public String rawDataToString(int lower, int upper) {
		if (lower < 0) lower = 0;
		if (upper > logData.size() - 1) upper = logData.size() - 1;
		StringBuilder sb = new StringBuilder();
		// a simple header has the logging interval in it
		sb.append("#logInterval: " + logInterval + "\r\n");
		for (int i = lower; i < upper; i++) sb.append(logData.get(i).rawDataToString() + "\r\n");
		return sb.toString();
	}

	public String rawDataToString() {
		return rawDataToString(0, logData.size());
	}
	
	// this mangles the data into the upload format - pretty cheezy hack
	public String rawDataToUploadString(int lower, int upper) {
		if (lower < 0) lower = 0;
		if (upper > logData.size() - 1) upper = logData.size() - 1;
		StringBuilder sb = new StringBuilder();
		for (int i = lower; i < upper; i++) sb.append(logData.get(i).rawDataToUploadString() + "\r\n");
		return sb.toString();
	}
	
	public void fromRawData(String rawData) throws IOException {
		BufferedReader br = new BufferedReader(new StringReader(rawData));
		Vector<String> headerLines = new Vector<String>();
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) headerLines.add(line.substring(1));
			else {
				LogEntry le = new LogEntry();
				le.fromRawData(line);
				logData.add(le);
			}
		}
		calculateAltitudes();
		// find the logging interval, if present - otherwise default is used
		for (String l : headerLines) {
			if (l.startsWith("logInterval")) logInterval = Double.parseDouble(l.substring(14));
		}
	}
}
