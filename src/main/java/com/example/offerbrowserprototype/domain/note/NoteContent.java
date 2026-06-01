package com.example.offerbrowserprototype.domain.note;

import java.util.List;

public class NoteContent {

    private String title;
    private String summary;
    private String simpleExplanation;
    private List<Section> sections;
    private List<Example> examples;
    private List<String> commonMistakes;
    private List<String> memoryPoints;
    private List<String> suggestedSearchPhrases;

    public NoteContent() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSimpleExplanation() {
        return simpleExplanation;
    }

    public void setSimpleExplanation(String simpleExplanation) {
        this.simpleExplanation = simpleExplanation;
    }

    public List<Section> getSections() {
        return sections;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    public List<Example> getExamples() {
        return examples;
    }

    public void setExamples(List<Example> examples) {
        this.examples = examples;
    }

    public List<String> getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(List<String> commonMistakes) {
        this.commonMistakes = commonMistakes;
    }

    public List<String> getMemoryPoints() {
        return memoryPoints;
    }

    public void setMemoryPoints(List<String> memoryPoints) {
        this.memoryPoints = memoryPoints;
    }

    public List<String> getSuggestedSearchPhrases() {
        return suggestedSearchPhrases;
    }

    public void setSuggestedSearchPhrases(List<String> suggestedSearchPhrases) {
        this.suggestedSearchPhrases = suggestedSearchPhrases;
    }

    public static class Section {
        private String heading;
        private String content;

        public Section() {
        }

        public String getHeading() {
            return heading;
        }

        public void setHeading(String heading) {
            this.heading = heading;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class Example {
        private String title;
        private String code;
        private String explanation;

        public Example() {
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }
}
