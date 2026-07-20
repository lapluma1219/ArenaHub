package com.arenahub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arenahub.dto.AuthDtos.RegisterRequest;
import com.arenahub.dto.RoomDtos.MatchRecordResponse;
import com.arenahub.dto.RoomDtos.MatchmakingResponse;
import com.arenahub.exception.BusinessException;
import com.arenahub.service.AuthService;
import com.arenahub.service.MatchmakingService;
import com.arenahub.service.PlayerService;
import com.arenahub.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ArenaHubFlowTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private MatchmakingService matchmakingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private PlayerService playerService;

    @Test
    void shouldCompleteMatchmakingAndSettlementFlow() {
        var alice = authService.register(new RegisterRequest("alice_flow", "password1", "Alice"));
        var bob = authService.register(new RegisterRequest("bob_flow", "password2", "Bob"));

        MatchmakingResponse aliceJoin = matchmakingService.join(alice.player().id());
        MatchmakingResponse bobJoin = matchmakingService.join(bob.player().id());

        assertThat(aliceJoin.state()).isEqualTo("WAITING");
        assertThat(bobJoin.state()).isEqualTo("MATCHED");
        assertThat(bobJoin.roomId()).isNotNull();

        MatchRecordResponse record = roomService.finishRoom(bobJoin.roomId(), alice.player().id(), alice.player().id());

        assertThat(record.winner().id()).isEqualTo(alice.player().id());
        assertThat(record.winnerRatingAfter()).isEqualTo(1025);
        assertThat(record.loserRatingAfter()).isEqualTo(985);
        assertThat(roomService.myMatches(alice.player().id())).hasSize(1);
        assertThat(playerService.leaderboard(2).get(0).id()).isEqualTo(alice.player().id());

        assertThatThrownBy(() -> roomService.finishRoom(
                bobJoin.roomId(), bob.player().id(), bob.player().id()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("房间已结算");

        assertThat(roomService.myMatches(alice.player().id())).hasSize(1);
        assertThat(playerService.getMe(alice.player().id()).rating()).isEqualTo(1025);
        assertThat(playerService.getMe(bob.player().id()).rating()).isEqualTo(985);
    }
}
