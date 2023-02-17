package flwr.android_client;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

public class Qlearning  {

    TextView result;
    // Define the state-action space
    private int states;
    //Counter measures to recommend
    private int actions;

    String []action_name={"Breathing for 1 min","Meditation for 1 min","Build a gratitude list","Praise yourself"};

    // Initialize the Q-table with random values
    final double[][] qTable;

    // Define the learning rate and discount factor
    private double alpha = 0.1;
    private double gamma = 0.9;

    public Qlearning(int states,int actions) {
        this.states = states;
        this.actions = actions;

        qTable = new double[states][actions];
        Random rand = new Random();
        for (int i = 0; i < states; i++) {
            for (int j = 0; j < actions; j++) {
                qTable[i][j] = rand.nextDouble();
            }
        }
    }

    public int getAction(int state) {
        double epsilon = 0.1;
        Random rand = new Random();
        if (rand.nextDouble() < epsilon) {
            // choose a random action with probability epsilon
            return rand.nextInt(actions);
        } else {
            // choose the action with the highest Q-value with probability 1 - epsilon
            int action = 0;
            double maxValue = Double.MIN_VALUE;
            for (int i = 0; i < actions; i++) {
                if (qTable[state][i] > maxValue) {
                    maxValue = qTable[state][i];
                    action = i;
                }
            }
            return action;
        }
    }

    public void takeAction(int state, int action,Context context) {
        int nextState = state;
        int reward ;

        // implement action

        reward=showEffectivenessPrompt(state,action,context);

        if(reward>=8)
            nextState=Math.max(state-2,0);
        else if(reward>=6)
            nextState=Math.max(state-1,0);
        else
            nextState=state;

        updateState(state,action,reward,nextState);
    }

    private int showEffectivenessPrompt(int state,int action,Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

//        ImageView imageView = new ImageView(context);
//        imageView.setImageResource(R.drawable.breathe);

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog, null);

        result=dialogView.findViewById(R.id.result1);
        result.setText("Stress Level - "+state);

        builder.setTitle(action_name[action]);


        EditText input = dialogView.findViewById(R.id.reward);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
//        builder.setMessage("");
        builder.setView(dialogView);
//        final EditText input = new EditText(context);
//        input.setInputType(InputType.TYPE_CLASS_NUMBER);
//        builder.setView(input);
        final int[] reward = new int[1];
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // retrieve the value entered by the user
                int effectiveness = Integer.parseInt(input.getText().toString());
                // use the value to adjust the reward
                reward[0] =effectiveness;
            }
        });
//        builder.setView(imageView);

        builder.create().show();
        return reward[0];
    }
    //Update occurs after an action is taken
    public void updateState(int previousState, int action, double reward, int nextState) {
        double[] nextActionValues = qTable[nextState];
        double max = nextActionValues[0];
        for (int i = 1; i < actions; i++) {
            if (max < nextActionValues[i]) max = nextActionValues[i];
        }
        qTable[previousState][action] += alpha * (reward + (gamma * max));

    }
}
