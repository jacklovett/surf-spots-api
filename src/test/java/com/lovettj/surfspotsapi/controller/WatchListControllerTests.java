package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.service.WatchListService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WatchListControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private WatchListService watchListService;

  @Test
  void testGetUsersWatchList() throws Exception {
    SurfSpot spot1 = new SurfSpot();
    SurfSpot spot2 = new SurfSpot();
    List<SurfSpot> spots = Arrays.asList(spot1, spot2);

    when(watchListService.getUsersWatchList(anyLong())).thenReturn(spots);

    mockMvc.perform(get("/api/spots-watchList/1"))
        .andExpect(status().isOk()); // Expected 200 status code

    verify(watchListService).getUsersWatchList(anyLong());
  }
}
