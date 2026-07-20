package com.arenahub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private GameRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "winner_id", nullable = false)
    private PlayerProfile winner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loser_id", nullable = false)
    private PlayerProfile loser;

    @Column(nullable = false)
    private int winnerRatingAfter;

    @Column(nullable = false)
    private int loserRatingAfter;

    @Column(nullable = false)
    private LocalDateTime finishedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public GameRoom getRoom() {
        return room;
    }

    public void setRoom(GameRoom room) {
        this.room = room;
    }

    public PlayerProfile getWinner() {
        return winner;
    }

    public void setWinner(PlayerProfile winner) {
        this.winner = winner;
    }

    public PlayerProfile getLoser() {
        return loser;
    }

    public void setLoser(PlayerProfile loser) {
        this.loser = loser;
    }

    public int getWinnerRatingAfter() {
        return winnerRatingAfter;
    }

    public void setWinnerRatingAfter(int winnerRatingAfter) {
        this.winnerRatingAfter = winnerRatingAfter;
    }

    public int getLoserRatingAfter() {
        return loserRatingAfter;
    }

    public void setLoserRatingAfter(int loserRatingAfter) {
        this.loserRatingAfter = loserRatingAfter;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
}
