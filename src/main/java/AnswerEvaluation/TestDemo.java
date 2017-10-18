package AnswerEvaluation;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.util.*;

/**
 * Created by shankaragarwal on 30/06/17.
 */
public class TestDemo {

    StanfordCoreNLP pipeline;

    public TestDemo(){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie,ner,parse,dcoref,dcoref,entitymentions,entitylink");
        pipeline = new StanfordCoreNLP(props);

    }

    public void test() throws IOException {

        String testString = cleanString("Football is played by Anil . Anil is a good soccer player");
        String answer = cleanString("Anil plays football and Anil is a good soccer player");
        System.out.println(testString);
        System.out.println(answer);

        List<RelationTriple> testTriples = breakIntoSentencesOpenNlp(testString);
        List<RelationTriple> answerTriples = breakIntoSentencesOpenNlp(answer);

        float score = 0.0f;

        System.out.println(answerTriples);
        System.out.println(testTriples);

//        for(RelationTriple answerTriple:answerTriples){
//            for(RelationTriple testTriple:testTriples){
//                if(equality(answerTriple,testTriple)){
//                    score += 1.0f;
//                }
//            }
//        }

//        score = score/(answerTriples.size());
        System.out.println(score);

    }
    public static void main(String[] args) throws Exception {
        TestDemo testDemo = new TestDemo();
        testDemo.test();
    }

    private String cleanDocument(String path) throws IOException {
        InputStream is = new FileInputStream(path);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));
        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();
        while(line != null){
            sb.append(line).append("\n");
            line = buf.readLine();
        }
        String fileAsString = sb.toString();

        return solveCoreferences(fileAsString);

    }

    private String cleanString(String str){
        return solveCoreferences(str);
    }

    private String solveCoreferences(String text)
    {

        Annotation doc = new Annotation(text);
        pipeline.annotate(doc);


        Map<Integer, CorefChain> corefs = doc.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);


        List<String> resolved = new ArrayList<>();

        for (CoreMap sentence : sentences) {

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            for (CoreLabel token : tokens) {

                Integer corefClustId= token.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);


                CorefChain chain = corefs.get(corefClustId);


                if(chain==null){
                    resolved.add(token.word());
                }else{

                    int sentINdx = chain.getRepresentativeMention().sentNum -1;
                    CoreMap corefSentence = sentences.get(sentINdx);
                    List<CoreLabel> corefSentenceTokens = corefSentence.get(CoreAnnotations.TokensAnnotation.class);
                    StringBuilder newwords = new StringBuilder();
                    CorefChain.CorefMention reprMent = chain.getRepresentativeMention();
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    if (isPronoun(pos) && token.index() <= reprMent.startIndex || token.index() >= reprMent.endIndex) {

                        for (int i = reprMent.startIndex; i < reprMent.endIndex; i++) {
                            CoreLabel matchedLabel = corefSentenceTokens.get(i - 1);
                            resolved.add(matchedLabel.word().replace("'s", ""));
                            newwords.append(matchedLabel.word()).append(" ");

                        }
                    }

                    else {
                        resolved.add(token.word());
                    }



                }



            }

        }


        StringBuilder resolvedStr = new StringBuilder();
        System.out.println();
        for (String str : resolved) {
            resolvedStr.append(str).append(" ");
        }
//        System.out.println(resolvedStr);

        return resolvedStr.toString();
    }

    private static boolean equality(RelationTriple answerTriple, RelationTriple testTriple) {

        System.out.println(answerTriple);
        System.out.println(testTriple);

        List<CoreLabel> subjectAnswer = answerTriple.canonicalSubject;
        List<CoreLabel> testAnswer = testTriple.canonicalSubject;

//        System.out.println("Subject Answers");
//        printLabels(subjectAnswer);

//        System.out.println("Subject Test");
//        printLabels(subjectAnswer);

        double confidenceAnswer =answerTriple.confidence;
        double confidenceTest =testTriple.confidence;

        List<CoreLabel> answerRelations = answerTriple.relation;
        List<CoreLabel> testRelations = testTriple.relation;

//        System.out.println("Relations Answers");
//        printLabels(answerRelations);

//        System.out.println("Relations Test");
//        printLabels(testRelations);

        List<CoreLabel> objectAnswer = answerTriple.canonicalObject;
        List<CoreLabel> objectTest = testTriple.canonicalObject;

//        System.out.println("Relations Answers");
//        printLabels(objectAnswer);

//        System.out.println("Relations Test");
//        printLabels(objectTest);

        return false;
    }

    private static void printLabels(List<CoreLabel> labels) {
        for(CoreLabel label:labels)
            System.out.println(label);
    }

    public List<RelationTriple> breakIntoSentencesOpenNlp(String text) throws  IOException
    {


        Annotation document = new Annotation(text);

        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        List<RelationTriple> resultTriples = new ArrayList<>();

        for(CoreMap coreMap: sentences) {
            Sentence sentence = new Sentence(coreMap);
            try {
                Collection<RelationTriple> triples = sentence.openieTriples();
                resultTriples.addAll(triples);
            }
            catch (Exception exp){
                exp.printStackTrace();
            }

        }
        return resultTriples;
    }

    private boolean isPronoun(String pos) {
        return pos.equals("PRP");
    }

}
