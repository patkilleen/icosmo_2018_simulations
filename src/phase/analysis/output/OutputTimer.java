package phase.analysis.output;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import common.Algorithm;
import common.Timer;
import common.event.HistoryEvent;
import common.event.PerformanceMetricEvent;
import common.event.PhaseBeginEvent;
import common.event.PhaseCompleteEvent;
import common.event.stream.HistoryInputStream;
import common.event.stream.PerformanceMetricInputStream;
import common.exception.ConfigurationException;
import common.log.Logger;
import common.log.LoggerFactory;

public class OutputTimer extends Timer {


	private final static String CSV_HEADER_ROW = "experiment id,algorithm_id,algorith_name,tp,tn,fp,fn,tp rate,fp rate,threshold,accuracy,fscore";
	private static final String NEW_LINE = "\r\n";
	//private static final String R_SCRIPT_COMMAND = "C:\\Users\\Not admin\\Documents\\R\\R-3.6.0\\bin\\Rscript.exe ";
	//private static final String R_SCRIPT_COMMAND = "C:\\Dev\\icosmo_sim\\input\\run.bat";
	//private static final String R_SCRIPT_COMMAND = "rscript ";
	
	//input
	private PerformanceMetricInputStream performanceMetricInputStream;
	
	private HistoryInputStream historyInputStream;

	private boolean isSavingHistory;
	//parameters
	private int experimentId;

	//input files
	/**
	 * the file path of the configuration file used to start the simulation
	 */
	private String inputConfigFilePath;

	/**
	 * log file of the simulation
	 */
	private String inputLogFilePath;

	//output files
	/**
	 * the directory that output directories are created
	 */
	private String outputDirectory;

	/**
	 * the name of log file where the log will be moved to in output directory
	 */
	private String outputLogFileName;

	/**
	 * the name of the configuration file where it will be copied to in output directory
	 */
	private String outputConfigFileName;


	/**
	 * csv file path where the points of ROC curve are stored in output directory
	 */
	private String outputRocCSVFileName;


	//R cran configuration files
	/**
	 * the file path to the r script that will generate the roc curve 
	 */
	private String inputRocRScriptFilePath;


	/**
	 * path to roc curve image file generated by R Cran 
	 */
	private String outputRocCurveImageFileName;

	private String historyOuputFileName;
		
	//the directory with all input files to make a copy of
	private String inputDirectory;
	
	//assosications
	private List<Algorithm> algorithms;
	private String inputBatchFilePath;

	
	

	public OutputTimer(PerformanceMetricInputStream performanceMetricInputStream,int experimentId,
			String inputConfigFilePath, String inputLogFilePath, String outputFileDirectory, String outputLogFileName,
			String outputConfigFileName, String outputRocCSVFileName, String inputRocRScriptFilePath,
			String outputRocCurveImageFileName, String inputDirectory,String historyOuputFileName, String inputBatchFilePath,List<Algorithm> algorithms) {
		init(performanceMetricInputStream, experimentId,
				 inputConfigFilePath, inputLogFilePath, outputFileDirectory, outputLogFileName,
				 outputConfigFileName, 	outputRocCSVFileName, inputRocRScriptFilePath,
				 outputRocCurveImageFileName, inputDirectory,historyOuputFileName, inputBatchFilePath,algorithms);
	}

	/**
	 * empty constructor for subclass flexibility
	 */
	protected OutputTimer(){
		
	}
	
	protected void init(PerformanceMetricInputStream performanceMetricInputStream, int experimentId,
			String inputConfigFilePath, String inputLogFilePath, String outputFileDirectory, String outputLogFileName,
			String outputConfigFileName, String outputRocCSVFileName, String inputRocRScriptFilePath,
			String outputRocCurveImageFileName, String inputDirectory,String historyOuputFileName,  String inputBatchFilePath,List<Algorithm> algorithms) {
		if(algorithms == null || algorithms.isEmpty()){
			throw new ConfigurationException("cannot crate output timer due to empty algorithms");
		}
		if(inputConfigFilePath == null||
				inputLogFilePath == null ||
				outputFileDirectory== null ||
				outputLogFileName== null ||
				outputConfigFileName== null ||
						inputRocRScriptFilePath== null ||
								inputRocRScriptFilePath== null || inputBatchFilePath == null||
						outputRocCSVFileName== null){
			throw new ConfigurationException("cannot crate output timer due to null file path");
		}
		this.performanceMetricInputStream = performanceMetricInputStream;
		this.experimentId = experimentId;
		this.inputConfigFilePath = inputConfigFilePath;
		this.inputLogFilePath = inputLogFilePath;
		this.outputDirectory = outputFileDirectory;
		this.outputLogFileName = outputLogFileName;
		this.outputConfigFileName = outputConfigFileName;
		this.outputRocCSVFileName = outputRocCSVFileName;
		this.inputRocRScriptFilePath = inputRocRScriptFilePath;
		this.outputRocCurveImageFileName = outputRocCurveImageFileName;
		this.inputDirectory = inputDirectory;
		this.historyOuputFileName = historyOuputFileName;
		this.inputBatchFilePath = inputBatchFilePath;
		this.algorithms = algorithms;
	}
	
	public void setIsSavingHistory(boolean f){
		 this.isSavingHistory = f;
	}
	public HistoryInputStream getHistoryInputStream(){
		return this.historyInputStream;
	}
	
	public void setHistoryInputStream(HistoryInputStream historyInputStream){
		this.historyInputStream =historyInputStream;
	}
	
	public String getInputFilesDirectory(){
		return this.inputDirectory;
	}
	public int getExperimentId() {
		return experimentId;
	}

	public String getInputConfigFilePath() {
		return inputConfigFilePath;
	}

	public String getInputLogFilePath() {
		return inputLogFilePath;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public String getOutputLogFileName() {
		return outputLogFileName;
	}

	public String getOutputConfigFileName() {
		return outputConfigFileName;
	}

	public String getOutputRocCSVFileName() {
		return outputRocCSVFileName;
	}

	public String getInputRocRScriptFilePath() {
		return inputRocRScriptFilePath;
	}

	public String getOutputRocCurveImageFileName() {
		return outputRocCurveImageFileName;
	}

	public List<Algorithm> getAlgorithms() {
		return algorithms;
	}

	/**
	 * Hook to be overridden by subclasses which is called when a new phase begins.
	 * @param e The event indicating a new phase began.
	 * @throws IOException 
	 */
	protected void phaseStarted(PhaseBeginEvent e)throws InterruptedException{
	
		Logger log = LoggerFactory.getInstance();
		log.log_debug("OutputTimer phase started");
		
	}

	/**
	 * Hook to be overridden by subclasses which is called when a phase completes.
	 * @param e The event indicating a phase finished.
	 */
	protected void phaseEnded(PhaseCompleteEvent e)throws InterruptedException{
		Logger log = LoggerFactory.getInstance();
		log.log_debug("OutputTimer phase ended, starting to output results.");
		
		try{
			//create all output files
			Path simulationOutputDir = createOutputFiles();

			//the path to csv output
			//create the csv output file
			Path outputCSV = Paths.get(simulationOutputDir.toString(),outputRocCSVFileName);
			//iterate all algorithms
			for(Algorithm alg: algorithms){


				Iterator<PerformanceMetricEvent> it = performanceMetricInputStream.iterator(alg);
				while(it.hasNext()){

					PerformanceMetricEvent metric = it.next();
					
					String output = this.performanceMetricToCSVRow(metric);
				
					FileHandler.append(outputCSV, output.getBytes());
					
				}//end iterate metrics
			}//end iterate algoritms
			
	
			generateROCCurve(simulationOutputDir);

			log.log_info("finished outputing results. Simulation output directory: "+simulationOutputDir.toString());
			
			
			//output the log now
			
			Path inputLog = Paths.get(this.inputLogFilePath);
			Path outputLog = Paths.get(simulationOutputDir.toString(),outputLogFileName);
			FileHandler.copy(inputLog,outputLog);
			
			}catch(IOException e2){
				e2.printStackTrace();
			}
	}
	
	public void generateROCCurve(Path simulationOutputDir){
		
		String cmd = inputBatchFilePath;
		
		Logger log = LoggerFactory.getInstance();
		

		ProcessBuilder pb = new ProcessBuilder(cmd,inputRocRScriptFilePath,outputRocCSVFileName , outputRocCurveImageFileName);
		
		pb.directory(simulationOutputDir.toFile());
		
		Process p;
		try {
			p = pb.start();
			p.waitFor();
			
			log.log_debug("finished creating rock curve:");
			log.log_debug(convert(p.getInputStream(),Charset.defaultCharset()));
			log.log_debug(convert(p.getErrorStream(),Charset.defaultCharset()));
			
		} catch (IOException | InterruptedException e) {
			
			e.printStackTrace();
			throw new RuntimeException("failed to create roc curve: "+e.getMessage());
		}
	
		
	}
	
	
	public String convert(InputStream inputStream, Charset charset) throws IOException {
		 
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {	
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		}
	 
		return stringBuilder.toString();
	}
	public Path createOutputDirecotry() throws IOException{
		//make time as output directory name file name
		Date date = new Date() ;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

		String basePath = this.outputDirectory;
		String relativePath =  dateFormat.format(date);//new directory is named using timestamp for uniqueness
		Path simulationOutputDir = Paths.get(basePath,relativePath);
	
		
		//create the output directory
		return FileHandler.mkdir(simulationOutputDir);
			
	}
	
	/**
	 * Creates an output directory, and the output files.
	 * 
	 * Creates a directory of following format:
	 * \/output directory
	 * 		> \/timerstampe
	 * 		 	>configFile.xml
	 * 			>output.log
	 * 			>rocPoints.csv
	 * @return Path to output directory
	 */
	public Path  createOutputFiles(){

		
		try{
			//create directory to output files to
			Path simulationOutputDir =  createOutputDirecotry(); 

			
			//create directory to dump copy of input files
			Path inputFileDestDir = Paths.get(simulationOutputDir.toString(),"input");
		//	FileHandler.mkdir(inputFileDestDir);
			FileHandler.copyFolder(Paths.get(this.getInputFilesDirectory()).toFile(), inputFileDestDir.toFile());
			
			//copy configuration file to output dir
			//Path inputConfig = Paths.get(this.inputConfigFilePath);
			//Path outputConfig = Paths.get(simulationOutputDir.toString(),outputConfigFileName);
			//FileHandler.copy( inputConfig,outputConfig);

			//copy log into output dir
			if(isSavingHistory){
				writeHistoryEvent(simulationOutputDir);
			}
			//create the csv output file
			Path outputCSV = Paths.get(simulationOutputDir.toString(),outputRocCSVFileName);
			outputCSV = FileHandler.createFile(outputCSV);

			String csvHeader = CSV_HEADER_ROW + NEW_LINE;
			//append the csv header to it
			FileHandler.append(outputCSV, csvHeader.getBytes());
			
			
			
			return simulationOutputDir;
		}catch(IOException e){
			e.printStackTrace();
		}
return null;
	}
	
	/**
	 * Serializes the hisotry event found in the history input stream and saves it to a file 
	 * @param outputDir The directory to save the history event.
	 * @throws IOException
	 */
	public void writeHistoryEvent(Path outputDir) throws IOException{
		
		Path outputFile = Paths.get(outputDir.toString(),historyOuputFileName);
		
		//now serialize the history
		HistoryEvent histEvent = this.historyInputStream.readHistoryEvent();
				
		if(histEvent == null){
			Logger log = LoggerFactory.getInstance();
			log.log_warning("history was null in history stream, cannot save it.");
			return;
		}
		HistoryEvent.writeHistoryEvent(outputFile.toString(), histEvent);
	}
	
	public String performanceMetricToCSVRow(PerformanceMetricEvent e){

		String result = "";
		//private final static String CSV_HEADER_ROW = "experiment id,algorithm,tp,tn,fp,fn,tp rate,fp rate,threshold,accuracy,fscore";
		result+= this.experimentId + ",";
		result+=e.getAlgorithm().getId() + ",";
		result+=e.getAlgorithm().getName()+ ",";
		result+=e.getTruePositiveCount() + ",";
		result+=e.getTrueNegativeCount() + "," ;
		result+= e.getFalsePositiveCount() + ",";
		result+=e.getFalseNegativeCount() + ",";
		result+=e.getTruePositiveRate()+",";
		result+=e.getFalsePositiveRate() + ",";
		result+=e.getAccuracy() + ",";
		result+= e.getFscore();
		result+=NEW_LINE;


		return result;

	}


	public PerformanceMetricInputStream getPerformanceMetricInputStream() {
		return performanceMetricInputStream;
	}
	public void setPerformanceMetricInputStream(PerformanceMetricInputStream performanceMetricInputStream) {
		this.performanceMetricInputStream = performanceMetricInputStream;
	}

}