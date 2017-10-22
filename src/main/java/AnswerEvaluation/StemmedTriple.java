package AnswerEvaluation;

import java.util.List;

/**
 * Created by shankaragarwal on 21/10/17.
 */
public class StemmedTriple {
    private List<String> subjects;
    private List<String> relation;
    private List<String> objects;

    private double score;

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public List<String> getRelation() {
        return relation;
    }

    public void setRelation(List<String> relation) {
        this.relation = relation;
    }

    public List<String> getObjects() {
        return objects;
    }

    public void setObjects(List<String> objects) {
        this.objects = objects;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
