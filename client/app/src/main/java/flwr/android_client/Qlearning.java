package flwr.android_client;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.opencsv.CSVReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

public class Qlearning  {

    TextView result;
    // Define the state-action space
    private int states;
    //Counter measures to recommend
    private int actions;

    String []action_name={"Breathing Exercise","Mindfulness","Gratitude","Stretching","Affirmation","Sensory Grounding","Guided Imagery","Laughter","Time in Nature","Muscle Relaxation"};
    String []action_detail={"Take a deep breath in through your nose for 4 seconds, hold for 4 seconds, and release slowly through your mouth for 4 seconds. Repeat this for 5-10 rounds.",
            "Take a moment to notice your surroundings and focus on your breath for 1-2 minutes. If your mind wanders, gently bring your attention back to your breath.",
            "Build a gratitude list","Take a break and do some simple stretches, like reaching your arms up over your head or stretching your neck from side to side.","Praise yourself","Take a moment to focus on your senses. Name one thing you can see, hear, feel, smell, and taste.","Imagine a peaceful scene, such as a beach or a forest, and picture yourself there. Focus on the details of the scene and take deep breaths as you do so.","Take a break to watch a funny video, read a joke, or call a friend who makes you laugh.",
    "Take a short walk outside and focus on the sounds, smells, and sights around you."," Tense up a muscle group (e.g., your shoulders) for 5-10 seconds, then release the tension and feel the difference. Move on to another muscle group and repeat the process."
            };
    // Initialize the Q-table with random values
    final float[][] qTable;

    // Define the learning rate and discount factor
    private double alpha = 0.1;
    private double gamma = 0.9;
    SharedPreferences sharedPref;

    public Qlearning(int states, int actions, Context context) {
        this.states = states;
        this.actions = actions;

        sharedPref = context.getSharedPreferences("qlearning", 0);
        int flag = sharedPref.getInt("Init", 0);
        qTable = new float[states][actions];
        if (flag == 0) {
            Random rand = new Random();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("Init", 1);
            for (int i = 0; i < states; i++) {
                for (int j = 0; j < actions; j++) {
                    qTable[i][j] = rand.nextFloat();
                    editor.putFloat("val" + String.valueOf(i) + String.valueOf(j), qTable[i][j]);
                }
            }
            editor.commit();
        } else if (flag == 1) {
            for (int i = 0; i < states; i++)
                for (int j = 0; j < actions; j++)
                    qTable[i][j] = sharedPref.getFloat("val" + String.valueOf(i) + String.valueOf(j), -1.0f);
        }
        Log.e("QLearing", Arrays.deepToString(qTable));
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
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog, null);

        result=dialogView.findViewById(R.id.result1);
        String stressLevel;
        switch (state) {
            case 0:
                stressLevel = "high";
                break;
            case 1:
                stressLevel = "medium high";
                break;
            case 2:
                stressLevel = "medium";
                break;
            case 3:
                stressLevel = "medium low";
                break;
            default:
                stressLevel= "low";
                break;
        }
        result.setText("Stress Level - "+stressLevel);

//        if(stressLevel.equals("low"))
//        {
//            builder.setTitle("Your stress level is ok");
//            builder.setMessage("Good going, just continue to work on your health");
//            builder.create().show();
//            return 10;
//        }
 //       else {
            builder.setTitle(action_name[action]);

            builder.setMessage(action_detail[action]);

            ImageView image = dialogView.findViewById(R.id.imageView);
            switch (action) {
                case 0:
                    image.setImageResource(R.drawable.breathe);
                case 1:
                    image.setImageResource(R.drawable.meditation);
                case 2:
                    image.setImageResource(R.drawable.gratitude);
                default:
                    image.setImageResource(R.drawable.breathe);
            }

            EditText input = dialogView.findViewById(R.id.reward);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);

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
                    reward[0] = effectiveness;
                }
            });

//        builder.setView(imageView);

            builder.create().show();
            return reward[0];
 //       }
    }
    //Update occurs after an action is taken
    public void updateState(int previousState, int action, double reward, int nextState) {
        float[] nextActionValues = qTable[nextState];
        double max = nextActionValues[0];
        for (int i = 1; i < actions; i++) {
            if (max < nextActionValues[i]) max = nextActionValues[i];
        }
        qTable[previousState][action] += alpha * (reward + (gamma * max));

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("val" + String.valueOf(previousState) + String.valueOf(action), qTable[previousState][action]);
        editor.commit();

        Log.e("QLearning", Arrays.deepToString(qTable));
    }
}
