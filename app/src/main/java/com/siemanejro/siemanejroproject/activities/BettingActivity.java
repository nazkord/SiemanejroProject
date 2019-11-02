package com.siemanejro.siemanejroproject.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.siemanejro.siemanejroproject.Adapters.RVMatchesAdapter;
import com.siemanejro.siemanejroproject.R;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import communication.Client;
import model.Bet;
import model.BetList;
import model.FullTimeResult;
import model.Match;
import model.Score;
import utils.NetworkUtil;
import utils.ResultUtil;

import static android.graphics.drawable.ClipDrawable.HORIZONTAL;

public class BettingActivity extends AppCompatActivity {

    /// Local variables ///

    Button saveButton;
    Button chooseDateButton;
    RVMatchesAdapter rvMatchesAdapter;
    Long leagueID;
    String leagueName;
    String selectedDate;

    RecyclerView rvBets;
    List<Match> allMatches;
    List<Bet> betsInRV = new ArrayList<>();
    BetList betList = new BetList();
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_betting);

        init();

        //get matches from API
        try {
            new LoadMatches().execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        rvBets = findViewById(R.id.matchesList);
        saveButton = (Button) findViewById(R.id.saveButton);
        chooseDateButton = findViewById(R.id.choose_date_button);
        Intent intent = getIntent();
        leagueID = intent.getLongExtra("leagueID", 0);
        leagueName = intent.getStringExtra("leagueName");
        setToolbarTitleAndBackPressButton(leagueName);
        saveButtonClicked();
        chooseDateClicked();
    }

    private void setToolbarTitleAndBackPressButton(String title) {
        getSupportActionBar().setTitle(title); // set title for toolbar
        getSupportActionBar().setDisplayShowHomeEnabled(true); //enable back press button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    /// -------- RecyclerView and Adapter methods -----------

    private void initializeRecyclerView() {

        //get data for RV
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        selectedDate = LocalDateTime.now().format(dateFormat);
        betsInRV = expandMatchesToBets(getMatchesFromSelectedDate(selectedDate));

        // Create adapter passing in bets with chosen matches
        rvMatchesAdapter = new RVMatchesAdapter((ArrayList<Bet>) betsInRV);

        DividerItemDecoration itemDecor = new DividerItemDecoration(getApplicationContext(), HORIZONTAL);
        rvBets.addItemDecoration(itemDecor);
        rvBets.setAdapter(rvMatchesAdapter);
        rvBets.setLayoutManager(linearLayoutManager);
    }

    private ArrayList<Bet> expandMatchesToBets(List<Match> matches) {
        return (ArrayList<Bet>) matches.stream()
                .map(m -> new Bet(null, m, null, null, null))
                .collect(Collectors.toList());
    }

    public List<Match> getMatchesFromSelectedDate(String date) {
        return allMatches.stream()
                .filter(Match -> Match.getUtcDate().substring(0,10).equals(date))
                .collect(Collectors.toList());
    }

    private void modifyListOfMatchesByDate(String dateInString) {
        //clear bets in adapter
        rvMatchesAdapter.notifyItemRangeRemoved(0, rvMatchesAdapter.getItemCount());
        betsInRV.clear();
        betsInRV.addAll(expandMatchesToBets(getMatchesFromSelectedDate(dateInString)));
        //notify of new bets inserted
        rvMatchesAdapter.notifyItemRangeInserted(0, betsInRV.size());
    }

    /// -------- Methods for saving bets -----------

    private void savedUserBets() {
        List<Bet> bets = getNewUserBets();
        betList.clear();
        betList.addAll(bets);
        new PostBets().execute();
    }

    private List<Bet> getNewUserBets() {
        View betView;
        Bet betItem;
        EditText userBet1;
        EditText userBet2;

        int numberOfMatches = rvMatchesAdapter.getItemCount();
        List<Bet> bets = new ArrayList<>();

        for (int i = 0; i < numberOfMatches; i++)
        {
            betItem = rvMatchesAdapter.getItem(i);
            betView = linearLayoutManager.findViewByPosition(i);


            userBet1 = (EditText) betView.findViewById(R.id.result1);
            userBet2 = (EditText) betView.findViewById(R.id.result2);
            if(userBet1.getText().toString().isEmpty() || userBet2.getText().toString().isEmpty())
                continue;
            Integer userBetResult1 = Integer.parseInt(userBet1.getText().toString());
            Integer userBetResult2 = Integer.parseInt(userBet2.getText().toString());

            Score userScore = new Score(null, getWinnerForScore(userBetResult1,userBetResult2),
                    new FullTimeResult(null, userBetResult1, userBetResult2));
            betItem.setUserScore(userScore);

            bets.add(betItem);
        }
        return bets;
    }

    private String getWinnerForScore(Integer a, Integer b) {
        if(a > b) {
            return "HOME_TEAM";
        } else if (b > a) {
            return "AWAY_TEAM";
        } else {
            return "DRAW";
        }
    }

    /// -------- onClicker's and DatePickerDialog -----------

    private void saveButtonClicked() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savedUserBets();
            }
        });
    }

    private void chooseDateClicked() {
        chooseDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePickerDialog();
            }
        });

    }

    private void openDatePickerDialog() {
        int year = Integer.parseInt(selectedDate.substring(0, 4));
        int monthOfYear = Integer.valueOf(selectedDate.substring(5,7)) - 1;
        int dayOfMonth = Integer.parseInt(selectedDate.substring(8, 10));

        // open dateDialogPicker
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                String monthString = ((Integer)(monthOfYear + 1)).toString();
                String dayString = ((Integer)dayOfMonth).toString();

                String dateInString = ((Integer)year).toString() + "-"
                        + makeStringHaveTwoDigits(monthString) + "-"
                        + makeStringHaveTwoDigits(dayString);

                modifyListOfMatchesByDate(dateInString);
                selectedDate = dateInString;
            }
        }, year, monthOfYear, dayOfMonth);
        datePickerDialog.show();

    }

    private String makeStringHaveTwoDigits(String string) {
        if(string.length() < 2) {
            return "0" + string;
        }
        return string;
    }


    /// -------- Background Threads -----------

    private class LoadMatches extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            if(!NetworkUtil.isNetworkConnectionAvailable((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE))) {
                return 0;
            }
            allMatches = Client.SIEMAJERO.get().getMatchesByCompetition(leagueID);
            if(allMatches == null) {
                //TODO: it could be an error by server side or there are just no matches at all
                return 1;
            } else {
                return 2;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            switch (integer) {
                case 0 : {
                    Toast toast = Toast.makeText(BettingActivity.this,"No internet connection", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                }
                case 1 : {
                    Toast toast = Toast.makeText(BettingActivity.this,"Something went wrong (server side)", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                }
                case 2: {
                    initializeRecyclerView();
                    break;
                }
            }
        }
    }

    private class PostBets extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            if(!NetworkUtil.isNetworkConnectionAvailable((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE))) {
                return 0;
            }
            if(Client.SIEMAJERO.get().postUsersBet(betList)) {
                return 1;
            } else {
                return 2;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            switch (integer) {
                case 0: {
                    Toast toast = Toast.makeText(BettingActivity.this,"No internet connection", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                }
                case 1: {
                    Toast toast = Toast.makeText(BettingActivity.this,"Data Saved", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                }
                case 2: {
                    Toast toast = Toast.makeText(BettingActivity.this,"Something went wrong", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                }
            }

        }
    }
}
