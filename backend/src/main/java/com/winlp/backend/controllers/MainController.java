package com.winlp.backend.controllers;

import com.winlp.backend.dtos.*;
import com.winlp.backend.entities.ComparativeQuestion;
import com.winlp.backend.entities.ObjectsAndAspects;
import com.winlp.backend.entities.SummaryFeedback;
import com.winlp.backend.repositories.ComparativeQuestionRepository;
import com.winlp.backend.repositories.ObjectsAndAspectsRepository;
import com.winlp.backend.repositories.SummaryFeedbackRepository;
import com.winlp.backend.services.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@RestController("/")
public class MainController {

    @Autowired
    private ComparativeQuestionRepository comparativeQuestionRepository;

    @Autowired
    private ObjectsAndAspectsRepository objectsAndAspectsRepository;

    @Autowired
    private SummaryFeedbackRepository summaryFeedbackRepository;

    @Autowired
    private ClusterService clusterService;
    private List<String> badWords = new ArrayList<>();

    public MainController() {
        // the file has the format
        /*
        badword1, badword2, badword3
        badword4
        badword5, badword6
        ...
         */
        try {
            File file = new File("./badwords.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] words = line.split(",");
                for (String word : words) {
                    badWords.add(word.trim());
                }
                // remove spaces
                badWords.replaceAll(String::trim);
                // remove empty strings
                badWords.removeIf(String::isEmpty);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ------------------- GET -------------------

    @GetMapping("/")
    public String index() { return "Welcome to comparative question answering API. Check Swagger for Docs"; }

    // ======================

    /**
     * endpoint for checking if a question is comparative
     */
    @GetMapping("/isComparative/{question}")
    public int isComparative(@PathVariable String question) {
        // check if question contains bad words
        for (String badWord : badWords) {
            if (question.contains(badWord)) {
                System.out.println("bad word found: " + badWord);
                return 2;
            }
        }
        return clusterService.isComparative(question) ? 1 : 0;
    }

    /**
     * endpoint for getting objects and aspects from a question
     */
    @GetMapping("/getObjectsAndAspects/{faster}/{question}")
    public ObjectsAndAspectsResponse getObjectsAndAspects(@PathVariable String question, @PathVariable String faster) {
        return clusterService.getObjectsAndAspects(question, Boolean.parseBoolean(faster));
    }

    /**
     * endpoint for getting cam arguments and object scores from objects and aspects
     */
    @GetMapping("/cam/{objects}/{aspects}/{numOfArguments}/{faster}")
    public CamResponse getCamArgumentsAndObjectScores(@PathVariable List<String> objects,
                                                      @PathVariable List<String> aspects,
                                                      @PathVariable int numOfArguments,
                                                      @PathVariable boolean faster
    ) {

        CamResponse r = clusterService.getComparisonArguments(objects, aspects, faster);
        ArrayList<Argument> arguments = new ArrayList<>((r.firstObjectArguments()).subList(0, Math.min(r.firstObjectArguments().size(), 5 * numOfArguments)));
        arguments.addAll(r.secondObjectArguments().subList(0, Math.min(r.secondObjectArguments().size(), 5 * numOfArguments)));
        SentenceIdentificationRequest request = new SentenceIdentificationRequest(
                objects.get(0),
                objects.get(1),
                arguments
        );
        SentenceIdentificationResponse response = clusterService.classifySentences(request);
        // return only first numOfArguments arguments
        ArrayList<Argument> arguments1 = new ArrayList<>(response.arguments1());
        ArrayList<Argument> arguments2 = new ArrayList<>(response.arguments2());
        if (response.arguments1().size() > numOfArguments)
            arguments1 = new ArrayList<>(response.arguments1().subList(0, numOfArguments));
        if (response.arguments2().size() > numOfArguments)
            arguments2 = new ArrayList<>(response.arguments2().subList(0, numOfArguments));
        r = new CamResponse(r.firstObjectScore(), arguments1, arguments2);
        return r;
    }

    /**
     * endpoint for getting summarised answer from cam arguments, using a post mapping and a request body
     */
    @PostMapping("/summarise/{object1}/{object2}")
    public ResponseEntity<SummaryResponse> summariseCamArguments(
            @PathVariable String object1,
            @PathVariable String object2,
            @RequestBody Arguments arguments) {
        SummaryResponse r = new SummaryResponse(clusterService.getSummary(arguments.arguments(),object1, object2));
        // remove all "\n" from the summary
        String summary = r.summary();
        summary = summary.replace("\\n\\n", "");
        summary = summary.replace("\\n", "");
        // remove \ before "
        summary = summary.replace("\\", "");
        System.out.println("summary: " + summary);
        r = new SummaryResponse(summary);
        return ResponseEntity.ok(r);
    }

    /**
     * endpoint for reporting a question and it's is comparative result
     */
    @GetMapping("/report/{isComparative}/{question}")
    public void report(@PathVariable String question, @PathVariable boolean isComparative) {
        System.out.println("reporting question: " + question + " as " + isComparative);
        comparativeQuestionRepository.save(new ComparativeQuestion(question, isComparative));
        comparativeQuestionRepository.flush();
    }

    /**
     * endpoint for reporting a question and it's objects and aspects
     */
    @GetMapping("/report/{object1}/{object2}/{aspects}/{question}")
    public void report(@PathVariable String question,
                       @PathVariable String object1,
                       @PathVariable String object2,
                       @PathVariable List<String> aspects) {
        System.out.println("reporting question: " + question + " with objects: " + object1 + ", " + object2 + " and aspects: " + aspects);
        while (aspects.size() < 5) {
            aspects.add("");
        }
        objectsAndAspectsRepository.save(new ObjectsAndAspects(question,
                object1, object2,
                aspects.get(0),
                aspects.get(1),
                aspects.get(2),
                aspects.get(3),
                aspects.get(4)));
        objectsAndAspectsRepository.flush();
    }

    /**
     * endpoint for recieving feedback on Summary
     */
    @PostMapping("/summary_feedback")
    public void summaryFeedback(@RequestBody SummaryFeedbackRequest request) {
        System.out.println("reporting summary feedback: " + request);
        summaryFeedbackRepository.save(new SummaryFeedback(
                request.question(),
                request.arguments(),
                request.summary(),
                request.useful(),
                request.fluent()
        ));
        summaryFeedbackRepository.flush();
    }

}
