package AnswerEvaluation;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Created by shankaragarwal on 22/10/17.
 */
public class WUPSimilarity {

    static String path = "/scratchd/home/shankar/AutomaticAnswerEvaluation/calculate_similarity.py";
    public static double similarity(String s1,String s2) {
        Runtime rt = Runtime.getRuntime();
        String command = String.format("python %s \"%s\" \"%s\"",path,s1,s2);
        Process proc = null;
        try {
            System.out.println(command);
            proc = rt.exec(command);
            proc.waitFor();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;

    }
}
