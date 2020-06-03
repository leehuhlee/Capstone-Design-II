package com.google.sample.cloudvision.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.sample.cloudvision.MainActivity;
import com.google.sample.cloudvision.R;

public class FeedbackActivity extends AppCompatActivity implements View.OnClickListener {
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        Button btn_submit_feedback = (Button)findViewById(R.id.btn_submit_feedback);
        btn_submit_feedback.setOnClickListener(this);

        Button btn_home = (Button)findViewById(R.id.btn_home);
        btn_home.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View view) {
        Intent i = null;
        switch (view.getId()) {
            case R.id.btn_submit_feedback:
                regFeedback();
                break;
            case R.id.btn_home:
                i = new Intent(this, MainActivity.class);
                startActivity(i);
                break;
            default:
                break;
        }
    }

    private void regFeedback() {
        EditText edit_nation = (EditText) findViewById(R.id.edit_nation);
        EditText edit_airline = (EditText) findViewById(R.id.edit_airline);
        EditText edit_stuff = (EditText) findViewById(R.id.edit_stuff);
        EditText edit_detail = (EditText) findViewById(R.id.edit_detail);

        if (edit_nation.getText().toString().length() == 0 || edit_airline.getText().toString().length() == 0
        || edit_stuff.getText().toString().length() == 0 || edit_detail.getText().toString().length() == 0) {
            Toast.makeText(this, "There is a blank in the input. Please check again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        FeedbackItem item = new FeedbackItem();
        item.setNation(edit_nation.getText().toString());
        item.setAirline(edit_airline.getText().toString());
        item.setStuff(edit_stuff.getText().toString());
        item.setDetail(edit_detail.getText().toString());

        // firebase db Feedback에 edittext로 입력 받은 feedback 내용 추가
        databaseReference.child("Feedback").push().setValue(item);
        Toast.makeText(this, "Feedback Transfer Completed",
                Toast.LENGTH_LONG).show();
    }
}