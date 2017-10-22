package AnswerEvaluation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by shankaragarwal on 21/10/17.
 */
public class FactReader {

    String mainPath = "/Users/shankaragarwal/project/AutomaticAnswerEvaluation/src/resources";

    public HashMap<String,Integer> readFacts(int question){
        String fileName = "/trec_2003/actual_facts/"+question+".txt";
        HashMap<String,Integer> facts = new HashMap<>();
        File file = getFile(fileName);

        try (Scanner scanner = new Scanner(file)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String [] arr = line.split("\t");
                int score =2;
                if(arr[1].equals("vital")){
                    score =1;
                }
                facts.put(arr[2],score);
            }

            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return facts;
    }

    public String readBestAnswer(int question){
        String fileName = "/trec_2003/best_answer/"+question+".txt";
        StringBuilder builder = new StringBuilder();
        File file = getFile(fileName);

        try (Scanner scanner = new Scanner(file)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                builder.append(line);
            }

            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    private File getFile(String fileName) {
//        ClassLoader classLoader = getClass().getClassLoader();
//        System.out.println(fileName);
//        return new File(classLoader.getResource(fileName).getFile());
        String fullFileName = mainPath+"/"+fileName;
        File file = new File(fullFileName);
        return file;
    }

    public FactReader(){

    }
}
