import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.service.impl.ProductServiceImpl;
import com.ghlzm.iot.device.vo.ProductDetailVO;
import org.mockito.Mockito;

public class RuntimeGroupProbe {
  public static void main(String[] args) {
    DeviceMapper deviceMapper = Mockito.mock(DeviceMapper.class);
    DeviceOnlineSessionService deviceOnlineSessionService = Mockito.mock(DeviceOnlineSessionService.class);
    ProductServiceImpl service = Mockito.spy(new ProductServiceImpl(deviceMapper, deviceOnlineSessionService));

    Product existing = new Product();
    existing.setId(1001L);
    existing.setProductKey("muddy-water-product");
    existing.setProductName("泥水位监测产品");
    existing.setProtocolCode("mqtt-json");
    existing.setNodeType(1);
    existing.setStatus(1);

    Mockito.doReturn(existing).when(service).getRequiredById(1001L);
    Mockito.doReturn(true).when(service).updateById(Mockito.any(Product.class));
    Mockito.doReturn(new ProductDetailVO()).when(service).getDetailById(1001L);

    ProductAddDTO dto = new ProductAddDTO();
    dto.setProductKey("muddy-water-product");
    dto.setProductName("泥水位监测产品");
    dto.setProtocolCode("mqtt-json");
    dto.setNodeType(1);
    dto.setStatus(1);
    dto.setMetadataJson("""
      {
        \"objectInsight\": {
          \"customMetrics\": [
            {
              \"identifier\": \"S1_ZT_1.battery_dump_energy\",
              \"displayName\": \"电池余量\",
              \"group\": \"runtime\",
              \"includeInTrend\": true,
              \"includeInExtension\": true
            }
          ]
        }
      }
      """);

    try {
      service.updateProduct(1001L, dto);
      System.out.println("PROBE_RESULT=SUCCESS");
    } catch (BizException ex) {
      System.out.println("PROBE_RESULT=BIZ_EXCEPTION");
      System.out.println("PROBE_MESSAGE=" + ex.getMessage());
    }
  }
}