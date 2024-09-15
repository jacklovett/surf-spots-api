package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.WishlistSurfSpot;
import com.lovettj.surfspotsapi.service.WishlistSurfSpotService;
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
class WishlistSurfSpotControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private WishlistSurfSpotService wishlistSurfSpotService;

  @Test
  void testGetUsersWishlist() throws Exception {
    WishlistSurfSpot spot1 = new WishlistSurfSpot();
    WishlistSurfSpot spot2 = new WishlistSurfSpot();
    List<WishlistSurfSpot> spots = Arrays.asList(spot1, spot2);

    when(wishlistSurfSpotService.getUsersWishlist(anyLong())).thenReturn(spots);

    mockMvc.perform(get("/api/spots-wishlist/1"))
        .andExpect(status().isOk()); // Expected 200 status code

    verify(wishlistSurfSpotService).getUsersWishlist(anyLong());
  }
}
