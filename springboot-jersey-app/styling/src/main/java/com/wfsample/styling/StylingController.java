package com.wfsample.styling;

import com.wfsample.common.BeachShirtsUtils;
import com.wfsample.common.dto.PackedShirtsDTO;
import com.wfsample.common.dto.ShirtDTO;
import com.wfsample.common.dto.ShirtStyleDTO;
import com.wfsample.common.dto.DeliveryStatusDTO;
import com.wfsample.service.DeliveryApi;
import com.wfsample.service.StylingApi;
import io.opentracing.Tracer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.core.Response;

import static java.util.stream.Collectors.toList;

/**
 * Driver for styling service which manages different styles of shirts and takes orders for a shirts
 * of a given style.
 *
 * @author Srujan Narkedamalli (snarkedamall@wavefront.com).
 */
public class StylingController implements StylingApi {
  private final DeliveryApi deliveryApi;
  private List<ShirtStyleDTO> shirtStyleDTOS;

  StylingController(Tracer tracer) throws IOException {
    String deliveryUrl = "http://localhost:50052";
    this.deliveryApi = BeachShirtsUtils.createProxyClient(deliveryUrl, DeliveryApi.class, tracer);
    shirtStyleDTOS = new ArrayList<>();
    ShirtStyleDTO dto = new ShirtStyleDTO();
    dto.setName("style1");
    dto.setImageUrl("style1Image");
    ShirtStyleDTO dto2 = new ShirtStyleDTO();
    dto2.setName("style2");
    dto2.setImageUrl("style2Image");
    shirtStyleDTOS.add(dto);
    shirtStyleDTOS.add(dto2);
  }

  public List<ShirtStyleDTO> getAllStyles() {
    return this.shirtStyleDTOS;
  }

  public Response makeShirts(String id, int quantity) {
    /*
     * TODO: Try to report the value of quantity using WavefrontHistogram.
     *
     * Viewing the quantity requested by various clients as a minute distribution and then applying
     * statistical functions (median, mean, min, max, p95, p99 etc.) on that data is really useful
     * to understand the user trend.
     */
    if (ThreadLocalRandom.current().nextInt(0, 5) == 0) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Failed to make shirts!").build();
    }
    String orderNum = UUID.randomUUID().toString();
    List<ShirtDTO> packedShirts = new ArrayList<>();
    for (int i = 0; i < quantity; i++) {
      packedShirts.add(new ShirtDTO(new ShirtStyleDTO(id, id + "Image")));
    }
    PackedShirtsDTO packedShirtsDTO = new PackedShirtsDTO(packedShirts.stream().
        map(shirt -> new ShirtDTO(
            new ShirtStyleDTO(shirt.getStyle().getName(), shirt.getStyle().getImageUrl()))).
        collect(toList()));
    Response deliveryResponse = deliveryApi.dispatch(orderNum, packedShirtsDTO);
    if (deliveryResponse.getStatus() < 400) {
      return Response.ok().entity(deliveryResponse.readEntity(DeliveryStatusDTO.class)).build();
    } else {
      return Response.status(deliveryResponse.getStatus()).entity("Failed to make shirts").build();
    }
  }
}
