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

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shankaragarwal on 30/06/17.
 */
public class TestDemo {

    private static final double THRESHOLD = 0.5;
    private static final double BETA = 5;
    StanfordCoreNLP pipeline;
    private static final int SUBJECT_WEIGHT = 200;
    private static final int RELATION_WEIGHT = 100;

    private Stemmer stemmer;

    public static void main(String[] args) throws Exception {
        FactReader factReader = new FactReader();
        HashMap<String,Integer> map = factReader.readFacts(3);
        String humanAnswer = factReader.readBestAnswer(3);
        TestDemo testDemo = new TestDemo();
        testDemo.test(map,humanAnswer,0);
    }

    public TestDemo(){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie,ner,parse,dcoref,dcoref,entitymentions,entitylink");
        pipeline = new StanfordCoreNLP(props);
        stemmer = new Stemmer();

    }

    public void test(HashMap<String,Integer> answers, String humanAnswer, double humanScore) throws IOException {


        humanAnswer =cleanString(humanAnswer);
        int nonWhiteSpace = nonWhiteSpace(humanAnswer);


        HashMap<List<RelationTriple>,Integer> systemAnswers = breakIntoSentencesOpenNlp(answers);
        List<RelationTriple> humanAnswers = breakIntoSentencesOpenNlp(humanAnswer);

//        for(RelationTriple triple:humanAnswers){
//            printRelationTriple(triple);
//        }

        HashMap<List<StemmedTriple>,FactInfo> stemmedSystemAnswers = new HashMap<>();

        Iterator it = systemAnswers.entrySet().iterator();
        int id =1;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            List<RelationTriple> triples = (List<RelationTriple>) pair.getKey();
            List<StemmedTriple> stemmedTriples = getStemmedTriples(triples);
            FactInfo factInfo = new FactInfo(id, (Integer) pair.getValue());
            stemmedSystemAnswers.put(stemmedTriples, factInfo);
        }

        List<StemmedTriple> stemmedHumanAnswers = getStemmedTriples(humanAnswers);

        for(StemmedTriple triple:stemmedHumanAnswers)
            printStemmedTriple(triple);

        System.out.println("The Answers");
        it = stemmedSystemAnswers.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry pair = (Map.Entry) it.next();
            List<StemmedTriple> triples = (List<StemmedTriple>) pair.getKey();
            for(StemmedTriple triple:triples)
                printStemmedTriple(triple);
        }


        double score = evaluateScore(stemmedHumanAnswers,stemmedSystemAnswers,nonWhiteSpace);

        System.out.println("Human Score "+ humanScore + " Algorithm score" + score);


    }

    private void printStemmedTriple(StemmedTriple triple) {
        List<String> subjects =  triple.getSubjects();
        List<String> relations =  triple.getRelation();
        List<String> objects =  triple.getObjects();

        for(String subject:subjects){
            System.out.print(subject+" ");
        }

        for(String relation:relations){
            System.out.print(relation+" ");
        }

        for(String object:objects){
            System.out.print(object+" ");
        }

        System.out.println();
    }

    private void printRelationTriple(RelationTriple triple) {
        List<CoreLabel> subjects =  triple.canonicalSubject;
        List<CoreLabel> relations =  triple.relation;
        List<CoreLabel> objects =  triple.object;

        for(CoreLabel subject:subjects){
            System.out.print(subject.word()+" ");
        }

        for(CoreLabel relation:relations){
            System.out.print(relation.word()+" ");
        }

        for(CoreLabel object:objects){
            System.out.print(object.word()+" ");
        }

        System.out.println();
    }

    private int nonWhiteSpace(String str) {
        int nonSpaceCount = 0;
        for (char c : str.toCharArray()) {
            if (c != ' ') {
                nonSpaceCount++;
            }
        }
        return nonSpaceCount;
    }

    private double evaluateScore(List<StemmedTriple> humanAnswers,
                                 HashMap<List<StemmedTriple>, FactInfo> stemmedSystemAnswers,
                                 int nonWhiteSpace) {

        HashMap<FactInfo,Double> factScore = new HashMap<>();
        for(StemmedTriple humanAnswer:humanAnswers){
            Iterator it = stemmedSystemAnswers.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry pair = (Map.Entry) it.next();
                FactInfo factInfo = (FactInfo) pair.getValue();
                List<StemmedTriple> triples = (List<StemmedTriple>) pair.getKey();
                double similarity = getSimilarity(triples,humanAnswer);
                double currentSimilarity =factScore.getOrDefault(factInfo,0.0);
                if(similarity>currentSimilarity){
                    factScore.put(factInfo,similarity);
                }
            }
        }

        Iterator it = factScore.entrySet().iterator();
        int r =0;
        int a =0;
        while (it.hasNext()){
            Map.Entry pair = (Map.Entry) it.next();
            FactInfo factInfo = (FactInfo) pair.getKey();
            double similarity = (double) pair.getValue();
            
            if(similarity>THRESHOLD){
                switch (factInfo.score){
                    case 1:
                        r++;
                        break;
                    case 2:
                        a++;
                }
            }

        }

        int R =0;

        it = stemmedSystemAnswers.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry pair = (Map.Entry) it.next();
            FactInfo factInfo = (FactInfo) pair.getValue();
            if(factInfo.score ==1)
                R++;
        }

        double recall = ((double)r)/R;
        double allowance = 100 * (r+a);
        double precision;

        if(nonWhiteSpace<allowance){
            precision = 1;
        }
        else {
            precision = 1- (nonWhiteSpace-allowance)/nonWhiteSpace;
        }
        double F = ((BETA*BETA +1)* precision*recall)/(BETA*BETA*precision + recall);
        return F;

    }

    private double getSimilarity(List<StemmedTriple> triples, StemmedTriple humanAnswer) {
        double score =0;
        double totalWeight = SUBJECT_WEIGHT+RELATION_WEIGHT;
        for(StemmedTriple triple:triples){
            double confidence = triple.getScore();
            String answerSubject = getStringFromList(triple.getSubjects());
            String humanSubjects = getStringFromList(humanAnswer.getSubjects());

            String answerObjects = getStringFromList(triple.getObjects());
            String humanObjects = getStringFromList(humanAnswer.getObjects());

            double subjectSimilarity =getSubjectSimilarity(answerSubject,answerObjects,
                    humanSubjects,humanObjects);


            List<String> inferredRelations = triple.getRelation();
            List<String> humanRelations = humanAnswer.getRelation();
            double relationSimilarity = getRelationSimilarity(inferredRelations,humanRelations);

            double similarity = (SUBJECT_WEIGHT/totalWeight)*subjectSimilarity +
                    (RELATION_WEIGHT/totalWeight)*relationSimilarity;

            similarity = similarity*confidence;

            if(similarity>score)
                score = similarity;
        }

        return score;
    }

    private double getSubjectSimilarity(String answerSubject, String answerObjects, String humanSubjects, String humanObjects) {
        double maxScore =0;
        maxScore = (getStringSimilarity(answerSubject,humanSubjects)+
                getStringSimilarity(answerObjects,humanObjects))/2;

        double score = (getStringSimilarity(answerSubject,humanObjects)+
                getStringSimilarity(answerObjects,humanSubjects))/2;

        if(score>maxScore)
            maxScore = score;
        return maxScore;
    }

    private double getRelationSimilarity(List<String> inferredRelations, List<String> humanRelations) {
        String inferredRelationString = getStringFromList(inferredRelations);
        String humanRelationString = getStringFromList(humanRelations);

        System.out.println(" Relation String ");
        System.out.println(" Inferred --- " + inferredRelationString);
        System.out.println(" Human --- " + humanRelationString);
        System.out.println("-----------------------------------------------");
        double similarity = WUPSimilarity.similarity(inferredRelationString,
                humanRelationString);
        System.out.println(similarity);
        return similarity;
    }

    private String getStringFromList(List<String> inferredRelations) {
        StringBuilder builder = new StringBuilder();
        for(String str:inferredRelations){
            builder.append(str+" ");
        }
        return builder.toString();
    }

    private double getStringSimilarity(String a,String b){
        return 0;
    }

    private List<StemmedTriple> getStemmedTriples(List<RelationTriple> triples) {

        List<StemmedTriple> result = new ArrayList<>();
        for(RelationTriple triple:triples){

            List<CoreLabel> subjects = triple.canonicalSubject;
            List<CoreLabel> relations = triple.relation;
            List<CoreLabel> objects = triple.object;
            List<String> stemmedSubjects = new ArrayList<>();
            List<String> stemmedRelations = new ArrayList<>();
            List<String> stemmedObjects  = new ArrayList<>();

            StemmedTriple stemmedTriple = new StemmedTriple();

            for(CoreLabel label:subjects){
                stemmedSubjects.add(stemmer.stem(label.word()));
            }

            for(CoreLabel coreLabel: relations){
                stemmedRelations.add(stemmer.stem(coreLabel.word()));
            }

            for(CoreLabel coreLabel: objects){
                stemmedObjects.add(stemmer.stem(coreLabel.word()));
            }
            stemmedTriple.setObjects(stemmedObjects);
            stemmedTriple.setRelation(stemmedRelations);
            stemmedTriple.setSubjects(stemmedSubjects);
            stemmedTriple.setScore(triple.confidence);
            result.add(stemmedTriple);
        }
        return result;
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
                    StringBuilder newWords = new StringBuilder();
                    CorefChain.CorefMention reprMent = chain.getRepresentativeMention();
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    if (isPronoun(pos) && (token.index() <= reprMent.startIndex || token.index() >= reprMent.endIndex)) {

                        for (int i = reprMent.startIndex; i < reprMent.endIndex; i++) {
                            CoreLabel matchedLabel = corefSentenceTokens.get(i - 1);
                            resolved.add(matchedLabel.word().replace("'s", ""));
                            newWords.append(matchedLabel.word()).append(" ");

                        }
                    }

                    else {
                        resolved.add(token.word());
                    }



                }



            }

        }


        StringBuilder resolvedStr = new StringBuilder();
        for (String str : resolved) {
            resolvedStr.append(str).append(" ");
        }
        return resolvedStr.toString();
    }


    public List<RelationTriple> breakIntoSentencesOpenNlp(String answer) throws  IOException
    {


        List<RelationTriple> resultTriples = new ArrayList<>();

        Pattern re = Pattern.compile("[^.!?\\s][^.!?]*(?:[.!?](?!['\"]?\\s|$)[^.!?]*)*[.!?]?['\"]?(?=\\s|$)", Pattern.MULTILINE | Pattern.COMMENTS);
        Matcher reMatcher = re.matcher(answer);
        while (reMatcher.find()) {
            String text = reMatcher.group();
            Annotation document = new Annotation(text);

            pipeline.annotate(document);

            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

            for (CoreMap coreMap : sentences) {
                Sentence sentence = new Sentence(coreMap);
                try {
//                System.out.println(sentence.text());
                    Collection<RelationTriple> triples = sentence.openieTriples();
                    resultTriples.addAll(triples);
                } catch (Exception exp) {
//                exp.printStackTrace();
                }

            }
        }
        return resultTriples;
    }

    public HashMap<List<RelationTriple>,Integer> breakIntoSentencesOpenNlp(HashMap<String,Integer> text) throws  IOException
    {

        HashMap<List<RelationTriple>,Integer> result = new HashMap<>();
        Iterator it = text.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String fact = (String) pair.getKey();
            Annotation document = new Annotation(fact);

            pipeline.annotate(document);

            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
            List<RelationTriple> resultTriples = new ArrayList<>();

            for (CoreMap coreMap : sentences) {
                Sentence sentence = new Sentence(coreMap);
                try {
//                    System.out.println(sentence.text());
                    Collection<RelationTriple> triples = sentence.openieTriples();
                    for(RelationTriple triple:triples){
                        printRelationTriple(triple);
                    }
                    resultTriples.addAll(triples);
                } catch (Exception exp) {
                    exp.printStackTrace();
                }

            }
            result.put(resultTriples, (Integer) pair.getValue());
        }

        return result;
    }

    private boolean isPronoun(String pos) {
        return pos.equals("PRP");
    }

    class FactInfo{
        int factID;
        int score;

        public FactInfo(int factID, int score){
            this.factID = factID;
            this.score = score;
        }
    }

}
