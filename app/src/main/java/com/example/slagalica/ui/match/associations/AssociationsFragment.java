package com.example.slagalica.ui.match.associations;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.GameStateRepository;
import com.example.slagalica.data.AssociationsRepository;
import com.example.slagalica.data.UserStatsRepository;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;

public class AssociationsFragment extends Fragment {

    private MatchViewModel sharedViewModel;
    private AssociationsViewModel associationsViewModel;

    private GameStateRepository gameStateRepo;
    private AssociationsRepository associationRepo;

    private final UserStatsRepository statsRepo = new UserStatsRepository();

    private ListenerRegistration gameListener;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Button[][] fieldButtons;
    private EditText etSolutionA, etSolutionB, etSolutionC, etSolutionD, etFinalSolution;

    private EditText[] columnInputs;
    private Button btnSubmitAnswer;

    private static final int COLOR_OPENED = Color.parseColor("#F4A261");
    private static final int COLOR_SOLVED = Color.parseColor("#2EC27E");

    private String matchId;
    private String gameKey;

    private boolean localRoundOver = false;
    private boolean statsWritten   = false;
    private boolean timerStarted = false;
    private boolean advanceCalled = false;

    private int activeFirestorePlayer = 1;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_associations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        associationsViewModel = new ViewModelProvider(this).get(AssociationsViewModel.class);

        gameStateRepo = new GameStateRepository();
        associationRepo = new AssociationsRepository();

        matchId = sharedViewModel.getMatchId();

        String phase = sharedViewModel.getCurrentFragment().getValue();
        gameKey = "ASOCIJACIJE_R1".equals(phase) ? "assoc_r1" : "assoc_r2";

        connectViews(view);
        setupFieldClicks();
        lockAllInputs();

        btnSubmitAnswer.setOnClickListener(v -> checkAnswer());

        initializeRoundIfNeeded();
        listenToGameState();
    }

    private void initializeRoundIfNeeded() {
        boolean isInitiator = "assoc_r1".equals(gameKey)
                ? sharedViewModel.getIsPlayer1()
                : !sharedViewModel.getIsPlayer1();

        if (matchId == null || !isInitiator) {
            return;
        }

        associationRepo.fetchActivePuzzles()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(
                                requireContext(),
                                "Nema asocijacija u bazi.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    int randomIndex = new Random().nextInt(querySnapshot.size());
                    DocumentSnapshot puzzleDoc = querySnapshot.getDocuments().get(randomIndex);

                    Map<String, Object> data = createInitialAssociationState(puzzleDoc);
                    gameStateRepo.set(matchId, gameKey, data);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Greška pri učitavanju asocijacije.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private Map<String, Object> createInitialAssociationState(DocumentSnapshot puzzleDoc) {
        Map<String, Object> data = new HashMap<>();

        int startingPlayer = "assoc_r1".equals(gameKey) ? 1 : 2;

        data.put("puzzleId", puzzleDoc.getId());
        data.put("fields", puzzleDoc.get("fields"));
        data.put("columnSolutions", puzzleDoc.get("columnSolutions"));
        data.put("finalSolution", puzzleDoc.getString("finalSolution"));

        data.put("currentPlayer", startingPlayer);
        data.put("activeColumn", -1);
        data.put("freeGuessMode", false);
        data.put("roundDone", false);
        data.put("finalSolved", false);

        for (int c = 0; c < 4; c++) {
            data.put("solvedCol" + c, false);

            for (int r = 0; r < 4; r++) {
                data.put("open_" + c + "_" + r, false);
            }
        }

        return data;
    }

    private void listenToGameState() {
        if (matchId == null) return;

        gameListener = gameStateRepo.listen(matchId, gameKey, (snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            if (!associationsViewModel.puzzleLoaded) {
                loadPuzzleFromSnapshot(snapshot);
            }

            if (!associationsViewModel.puzzleLoaded) {
                return;
            }

            startAssociationsTimerIfNeeded();

            Long cp = snapshot.getLong("currentPlayer");
            if (cp != null) {
                activeFirestorePlayer = cp.intValue();
                associationsViewModel.currentPlayer = activeFirestorePlayer;
            }

            Long activeCol = snapshot.getLong("activeColumn");
            associationsViewModel.activeColumn = activeCol != null ? activeCol.intValue() : -1;

            Boolean freeGuess = snapshot.getBoolean("freeGuessMode");
            associationsViewModel.freeGuessMode = Boolean.TRUE.equals(freeGuess);

            syncOpenedFields(snapshot);
            syncSolvedColumns(snapshot);
            syncFinalSolution(snapshot);

            Boolean done = snapshot.getBoolean("roundDone");
            if (Boolean.TRUE.equals(done)) {
                doAdvance();
            }

            refreshInputState();
            refreshFieldButtonsState();
        });
    }

    private void startAssociationsTimerIfNeeded() {
        if (timerStarted || localRoundOver) {
            return;
        }

        timerStarted = true;

        sharedViewModel.stopTimer();
        sharedViewModel.startRoundTimer(120, this::finishRound);
    }

    @SuppressWarnings("unchecked")
    private void loadPuzzleFromSnapshot(DocumentSnapshot snapshot) {
        Object rawFields = snapshot.get("fields");

        if (!(rawFields instanceof Map)) {
            return;
        }

        Map<String, Object> fieldsMap = (Map<String, Object>) rawFields;

        String[][] loadedFields = new String[4][4];
        String[] keys = {"A", "B", "C", "D"};

        for (int c = 0; c < 4; c++) {
            Object rawColumn = fieldsMap.get(keys[c]);

            if (!(rawColumn instanceof List)) {
                return;
            }

            List<?> columnFields = (List<?>) rawColumn;

            if (columnFields.size() < 4) {
                return;
            }

            for (int r = 0; r < 4; r++) {
                loadedFields[c][r] = String.valueOf(columnFields.get(r));
            }
        }

        Object rawSolutions = snapshot.get("columnSolutions");

        if (!(rawSolutions instanceof List)) {
            return;
        }

        List<?> solutionsList = (List<?>) rawSolutions;

        if (solutionsList.size() < 4) {
            return;
        }

        String[] loadedColumnSolutions = new String[4];

        for (int i = 0; i < 4; i++) {
            loadedColumnSolutions[i] = String.valueOf(solutionsList.get(i));
        }

        String loadedFinalSolution = snapshot.getString("finalSolution");

        if (loadedFinalSolution == null) {
            return;
        }

        associationsViewModel.loadPuzzle(
                loadedFields,
                loadedColumnSolutions,
                loadedFinalSolution
        );
    }

    private void syncOpenedFields(DocumentSnapshot snapshot) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                Boolean opened = snapshot.getBoolean("open_" + col + "_" + row);

                if (Boolean.TRUE.equals(opened) && !associationsViewModel.openedFields[col][row]) {
                    associationsViewModel.openedFields[col][row] = true;

                    fieldButtons[col][row].setText(associationsViewModel.fields[col][row]);
                    fieldButtons[col][row].setEnabled(false);
                    fieldButtons[col][row].setBackgroundTintList(
                            ColorStateList.valueOf(COLOR_OPENED)
                    );
                }
            }
        }
    }

    private void syncSolvedColumns(DocumentSnapshot snapshot) {
        for (int col = 0; col < 4; col++) {
            Boolean solved = snapshot.getBoolean("solvedCol" + col);

            if (Boolean.TRUE.equals(solved) && !associationsViewModel.solvedColumns[col]) {
                associationsViewModel.solvedColumns[col] = true;
                revealColumn(col);
                lockColumnInput(col);
            }
        }
    }

    private void syncFinalSolution(DocumentSnapshot snapshot) {
        Boolean fs = snapshot.getBoolean("finalSolved");

        if (Boolean.TRUE.equals(fs) && !associationsViewModel.finalSolved) {
            associationsViewModel.finalSolved = true;
            revealAll();
        }
    }

    private void connectViews(View view) {
        fieldButtons = new Button[][]{
                {view.findViewById(R.id.btnA1), view.findViewById(R.id.btnA2), view.findViewById(R.id.btnA3), view.findViewById(R.id.btnA4)},
                {view.findViewById(R.id.btnB1), view.findViewById(R.id.btnB2), view.findViewById(R.id.btnB3), view.findViewById(R.id.btnB4)},
                {view.findViewById(R.id.btnC1), view.findViewById(R.id.btnC2), view.findViewById(R.id.btnC3), view.findViewById(R.id.btnC4)},
                {view.findViewById(R.id.btnD1), view.findViewById(R.id.btnD2), view.findViewById(R.id.btnD3), view.findViewById(R.id.btnD4)}
        };

        etSolutionA = view.findViewById(R.id.etSolutionA);
        etSolutionB = view.findViewById(R.id.etSolutionB);
        etSolutionC = view.findViewById(R.id.etSolutionC);
        etSolutionD = view.findViewById(R.id.etSolutionD);
        etFinalSolution = view.findViewById(R.id.etFinalSolution);
        btnSubmitAnswer = view.findViewById(R.id.btnSubmitAnswer);

        columnInputs = new EditText[]{
                etSolutionA,
                etSolutionB,
                etSolutionC,
                etSolutionD
        };

        btnSubmitAnswer = view.findViewById(R.id.btnSubmitAnswer);
    }

    private void setupFieldClicks() {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                int c = column, r = row;
                fieldButtons[c][r].setOnClickListener(v -> openField(c, r));
            }
        }
    }

    private void openField(int column, int row) {
        if (!isMyTurn()) {
            return;
        }

        if (!associationsViewModel.puzzleLoaded) {
            return;
        }

        if (localRoundOver || associationsViewModel.finalSolved) {
            return;
        }

        if (associationsViewModel.activeColumn != -1) {
            Toast.makeText(
                    requireContext(),
                    "Već ste otvorili jedno polje u ovom potezu.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (associationsViewModel.freeGuessMode) {
            Toast.makeText(
                    requireContext(),
                    "Možete da pogađate rešenja dok ne pogrešite.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        boolean opened = associationsViewModel.openField(column, row);

        if (!opened) {
            return;
        }

        fieldButtons[column][row].setText(associationsViewModel.fields[column][row]);
        fieldButtons[column][row].setEnabled(false);
        fieldButtons[column][row].setBackgroundTintList(
                ColorStateList.valueOf(COLOR_OPENED)
        );

        unlockOnlyThisColumnAndFinal(column);

        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("open_" + column + "_" + row, true);
            updates.put("activeColumn", column);
            updates.put("freeGuessMode", false);

            gameStateRepo.update(matchId, gameKey, updates);
        }
    }

    private void checkAnswer() {
        if (!isMyTurn() || localRoundOver) {
            return;
        }

        if (!associationsViewModel.puzzleLoaded) {
            return;
        }

        if (associationsViewModel.finalSolved) {
            return;
        }

        if (!associationsViewModel.freeGuessMode && associationsViewModel.activeColumn == -1) {
            Toast.makeText(
                    requireContext(),
                    "Prvo otvorite neko polje.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!isEmpty(etFinalSolution)) {
            checkFinalAnswer();
            return;
        }

        for (int c = 0; c < 4; c++) {
            if (!isEmpty(columnInputs[c]) && columnInputs[c].isEnabled()) {
                checkColumnAnswer(c, columnInputs[c]);
                return;
            }
        }

        passTurn();
    }

    private void checkColumnAnswer(int column, EditText editText) {
        String answer = editText.getText().toString();

        if (associationsViewModel.checkColumnAnswer(column, answer)) {
            int points = associationsViewModel.solveColumn(column);

            editText.setText(associationsViewModel.columnSolutions[column]);
            editText.setTextColor(Color.WHITE);
            editText.setAlpha(1f);
            editText.setEnabled(false);
            editText.setBackgroundTintList(ColorStateList.valueOf(COLOR_SOLVED));

            revealColumn(column);
            unlockUnsolvedColumnsAndFinal();

            Toast.makeText(
                    requireContext(),
                    "Tačno! +" + points + " bodova",
                    Toast.LENGTH_SHORT
            ).show();

            if (matchId != null) {
                Map<String, Object> updates = new HashMap<>();

                updates.put("solvedCol" + column, true);
                updates.put("activeColumn", -1);
                updates.put("freeGuessMode", true);

                for (int r = 0; r < 4; r++) {
                    updates.put("open_" + column + "_" + r, true);
                }

                gameStateRepo.update(matchId, gameKey, updates);
            }
        } else {
            wrongAnswer();
        }
    }

    private void checkFinalAnswer() {
        String answer = etFinalSolution.getText().toString();

        if (associationsViewModel.checkFinalAnswer(answer)) {
            int points = associationsViewModel.solveFinal();

            etFinalSolution.setText(associationsViewModel.finalSolution);
            etFinalSolution.setEnabled(false);
            etFinalSolution.setBackgroundTintList(ColorStateList.valueOf(COLOR_SOLVED));

            revealAll();

            Toast.makeText(
                    requireContext(),
                    "Tačno konačno rešenje! +" + points + " bodova",
                    Toast.LENGTH_SHORT
            ).show();

            if (matchId != null) {
                Map<String, Object> updates = new HashMap<>();

                updates.put("finalSolved", true);
                updates.put("activeColumn", -1);
                updates.put("freeGuessMode", false);

                for (int c = 0; c < 4; c++) {
                    updates.put("solvedCol" + c, true);

                    for (int r = 0; r < 4; r++) {
                        updates.put("open_" + c + "_" + r, true);
                    }
                }

                gameStateRepo.update(matchId, gameKey, updates);
            }

            finishRound();
        } else {
            wrongAnswer();
        }
    }

    private void passTurn() {
        int next = activeFirestorePlayer == 1 ? 2 : 1;

        Toast.makeText(
                requireContext(),
                "Potez prelazi na igrača " + next,
                Toast.LENGTH_SHORT
        ).show();

        clearInputs();
        lockAllInputs();

        associationsViewModel.currentPlayer = next;
        associationsViewModel.resetTurn();
        refreshFieldButtonsState();

        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();

            updates.put("currentPlayer", next);
            updates.put("activeColumn", -1);
            updates.put("freeGuessMode", false);

            gameStateRepo.update(matchId, gameKey, updates);
        }
    }

    private void wrongAnswer() {
        int next = activeFirestorePlayer == 1 ? 2 : 1;

        Toast.makeText(
                requireContext(),
                "Nije tačno. Igra igrač " + next,
                Toast.LENGTH_SHORT
        ).show();

        clearInputs();
        lockAllInputs();

        associationsViewModel.currentPlayer = next;
        associationsViewModel.resetTurn();
        refreshFieldButtonsState();

        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();

            updates.put("currentPlayer", next);
            updates.put("activeColumn", -1);
            updates.put("freeGuessMode", false);

            gameStateRepo.update(matchId, gameKey, updates);
        }
    }

    private void finishRound() {
        if (localRoundOver || !isAdded()) {
            return;
        }

        localRoundOver = true;
        sharedViewModel.stopTimer();

        writeStats();

        sharedViewModel.addCurrentPlayerPoints(associationsViewModel.currentScore);

        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("roundDone", true);

            gameStateRepo.update(matchId, gameKey, updates);
        } else {
            doAdvance();
        }
    }

    private void writeStats() {
        if (statsWritten) return;
        statsWritten = true;
        String uid = statsRepo.getCurrentUid();
        if (uid != null) {
            boolean solved = associationsViewModel.finalSolved || (associationsViewModel.currentScore > 0);
            statsRepo.recordAsocijacije(uid, solved, associationsViewModel.currentScore);
        }
    }

    private void doAdvance() {
        if (advanceCalled) {
            return;
        }

        advanceCalled = true;

        handler.postDelayed(() -> {
            if (isAdded()) {
                sharedViewModel.advanceGamePhase();
            }
        }, 1000);
    }

    private boolean isMyTurn() {
        boolean iAmPlayer1 = sharedViewModel.getIsPlayer1();

        return (iAmPlayer1 && activeFirestorePlayer == 1)
                || (!iAmPlayer1 && activeFirestorePlayer == 2);
    }

    private void refreshInputState() {
        if (!isMyTurn()
                || localRoundOver
                || !associationsViewModel.puzzleLoaded
                || associationsViewModel.finalSolved) {
            lockAllInputs();
            return;
        }

        if (associationsViewModel.freeGuessMode) {
            unlockUnsolvedColumnsAndFinal();
            return;
        }

        if (associationsViewModel.activeColumn >= 0) {
            unlockOnlyThisColumnAndFinal(associationsViewModel.activeColumn);
            return;
        }

        lockAllInputs();
    }

    private void refreshFieldButtonsState() {
        boolean canOpenField = isMyTurn()
                && !localRoundOver
                && associationsViewModel.puzzleLoaded
                && !associationsViewModel.finalSolved
                && associationsViewModel.activeColumn == -1
                && !associationsViewModel.freeGuessMode;

        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                boolean alreadyOpened = associationsViewModel.openedFields[col][row];
                boolean columnSolved = associationsViewModel.solvedColumns[col];

                fieldButtons[col][row].setEnabled(
                        canOpenField && !alreadyOpened && !columnSolved
                );
            }
        }
    }

    private void revealColumn(int column) {
        for (int row = 0; row < 4; row++) {
            fieldButtons[column][row].setText(associationsViewModel.fields[column][row]);
            fieldButtons[column][row].setEnabled(false);
            fieldButtons[column][row].setBackgroundTintList(
                    ColorStateList.valueOf(COLOR_SOLVED)
            );
        }
    }

    private void revealAll() {
        for (int column = 0; column < 4; column++) {
            columnInputs[column].setText(associationsViewModel.columnSolutions[column]);
            columnInputs[column].setEnabled(false);
            columnInputs[column].setBackgroundTintList(
                    ColorStateList.valueOf(COLOR_SOLVED)
            );

            revealColumn(column);
        }

        etFinalSolution.setText(associationsViewModel.finalSolution);
        etFinalSolution.setEnabled(false);
        etFinalSolution.setBackgroundTintList(ColorStateList.valueOf(COLOR_SOLVED));
    }

    private void lockColumnInput(int column) {
        columnInputs[column].setText(associationsViewModel.columnSolutions[column]);
        columnInputs[column].setEnabled(false);
        columnInputs[column].setBackgroundTintList(ColorStateList.valueOf(COLOR_SOLVED));
    }

    private void lockAllInputs() {
        if (columnInputs != null) {
            for (EditText input : columnInputs) {
                input.setEnabled(false);
            }
        }

        if (etFinalSolution != null) {
            etFinalSolution.setEnabled(false);
        }
    }

    private void unlockOnlyThisColumnAndFinal(int column) {
        lockAllInputs();

        if (!associationsViewModel.solvedColumns[column]) {
            columnInputs[column].setEnabled(true);
        }

        etFinalSolution.setEnabled(true);
    }

    private void unlockUnsolvedColumnsAndFinal() {
        lockAllInputs();

        for (int c = 0; c < 4; c++) {
            boolean columnHasOpenedField = associationsViewModel.isColumnOpened(c);
            boolean columnNotSolved = !associationsViewModel.solvedColumns[c];

            if (columnHasOpenedField && columnNotSolved) {
                columnInputs[c].setEnabled(true);
            }
        }

        etFinalSolution.setEnabled(true);
    }

    private boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    private void clearInputs() {
        for (int c = 0; c < 4; c++) {
            if (!associationsViewModel.solvedColumns[c]) {
                columnInputs[c].setText("");
            }
        }

        if (!associationsViewModel.finalSolved) {
            etFinalSolution.setText("");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        handler.removeCallbacksAndMessages(null);

        if (gameListener != null) {
            gameListener.remove();
        }
    }
}