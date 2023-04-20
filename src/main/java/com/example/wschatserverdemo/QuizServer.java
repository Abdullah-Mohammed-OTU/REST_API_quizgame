package com.example.wschatserverdemo;


import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.websocket.EncodeException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;
import java.nio.file.Files;

import java.io.*;
import java.util.*;

@ServerEndpoint(value="/ws")
public class QuizServer {

    private Map<String, String> usernames = new HashMap<String, String>();
    private Map<String, Integer> scores = new HashMap<>();
    public Map<String, Integer> attempts = new HashMap<String, Integer>();

    private HashMap<String, String> questionsAndAnswers = new HashMap<>();
    private List<String[]> questions = new ArrayList<String[]>();
    private Random random = new Random();
    private String currentQuestion;
    private String currentAnswer;

    @OnOpen
    public void open(Session session) throws IOException, EncodeException {
        session.getBasicRemote().sendText("{\"type\": \"quiz\", \"message\":\"(Server ): Welcome to the game. Please state your username to begin.\"}");
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        int score = 0;
        String userId = session.getId();
        if (usernames.containsKey(userId)) { // remove the user from the userlist
            String username = usernames.get(userId);
            usernames.remove(userId);
            for (Session peer : session.getOpenSessions()) {
                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + username + " left the game.\"}");
            }
        }
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
        String userID = session.getId();
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");
        int incorrectAttempts = 3;

        if (usernames.containsKey(userID)) { // not their first message
            String username = usernames.get(userID);
            System.out.println(username);
            for (Session peer : session.getOpenSessions()) { // prints the message for all the users to see
                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message + "\"}");
            }
            // Check if the message is an answer to the current question
            if (message.equalsIgnoreCase(currentAnswer)) {
                int score = scores.getOrDefault(userID, 0) + 1; // Increment the user's score
                scores.put(userID, score); // putting the score into the hashmap

                // Inform the user that they answered correctly
                session.getBasicRemote().sendText("{\"type\": \"quiz\", \"message\":\"(Server): Correct! Your score is now " + score + ".\"}");

                // Check if the user has won the game
                if (score == 10) {
                    for (Session peer : session.getOpenSessions()) {
                        if (!peer.getId().equals(userID)) {
                            peer.getBasicRemote().sendText("{\"type\": \"quiz\", \"message\":\"(Server): " + username + " won the game!\"}");
                        }
                    }
                    return;
                }

                // Ask the next question
                askQuestion(session);

            } else {
                // Increment the user's incorrect attempts count
                int attemptCount = attempts.getOrDefault(userID, 0) + 1;
                attempts.put(userID, attemptCount);

                // Check if the user has exceeded the maximum number of attempts
                if (attemptCount >= incorrectAttempts) {
                    // Inform the user that they have exceeded the maximum number of attempts
                    session.getBasicRemote().sendText("{\"type\": \"quiz\", \"message\":\"(Server): You have exceeded the maximum number of attempts. The correct answer was " + currentAnswer + ".\"}");

                    // Reset the user's attempt count and ask the next question
                    attempts.put(userID, 0);
                    askQuestion(session);
                } else {
                    // Inform the user that their answer was incorrect and prompt them to try again
                    session.getBasicRemote().sendText("{\"type\": \"quiz\", \"message\":\"(Server): Incorrect. Please try again.\"}");
                }
            }
        } else { //first message is their username
            usernames.put(userID, message);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome, " + message + "!\"}");
            for (Session peer : session.getOpenSessions()) {
                if (!peer.getId().equals(userID)) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + message + " has joined the game.\"}");
                }
            }
            askQuestion(session);
        }
    }

    private void askQuestion(Session session) throws IOException, EncodeException {
        // adding questions to the hashmap
        questionsAndAnswers.put("What is 5+7", "12");
        questionsAndAnswers.put("What is 5+3", "8");
        questionsAndAnswers.put("What is 3+7", "10");
        questionsAndAnswers.put("What is 3+4", "7");
        questionsAndAnswers.put("What is 1+1", "2");
        questionsAndAnswers.put("What is 3-1", "2");
        questionsAndAnswers.put("What is 9+10", "19");
        questionsAndAnswers.put("What is 80+87", "167");
        questionsAndAnswers.put("What is 132+1", "133");
        questionsAndAnswers.put("What is 23+32", "55");
        // Select a random question that hasn't been asked recently
        String[] questions = questionsAndAnswers.keySet().toArray(new String[0]); //creating array questions using the keys from the questionsandanswers hashmap
        String question = ""; //init question
        Random random = new Random(); // using the random library to ask random questions
        while (question.isEmpty() || question.equals(currentQuestion)) {
            question = questions[random.nextInt(questions.length)]; // this question is random from the csv file
        }

        // Set the current question and answer
        currentQuestion = question;
        currentAnswer = questionsAndAnswers.get(question).toString();

        // Send the question to the user
        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + currentQuestion + "\"}");
    }


}