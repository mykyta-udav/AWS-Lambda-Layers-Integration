package com.task09;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import com.task09.sdk.OpenMeteoSDK;

import java.util.Map;

@LambdaHandler(
		lambdaName = "api_handler",
		roleName = "api_handler-role",
		isPublishVersion = true,
		aliasName = "${lambdas_alias_name}",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
		layers = {"open_meteo_sdk"}
)
@LambdaLayer(
		layerName = "open_meteo_sdk",
		libraries = {"lib/task09-1.0.0.jar"}
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private final OpenMeteoSDK meteoSDK = new OpenMeteoSDK();
	private final Gson gson = new Gson();

	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
		// Correctly get path and method from the V2 event
		String path = request.getRawPath();
		String httpMethod = request.getRequestContext().getHttp().getMethod();

		if ("/weather".equals(path) && "GET".equalsIgnoreCase(httpMethod)) {
			try {
				String weatherForecast = meteoSDK.getWeatherForecast();
				return APIGatewayV2HTTPResponse.builder()
						.withStatusCode(200)
						.withHeaders(Map.of("Content-Type", "application/json"))
						.withBody(weatherForecast)
						.build();
			} catch (Exception e) {
				context.getLogger().log("Error fetching weather: " + e.getMessage());
				return APIGatewayV2HTTPResponse.builder()
						.withStatusCode(500)
						.withBody("{\"message\":\"Internal Server Error\"}")
						.build();
			}
		} else {
			String message = String.format("Bad request syntax or unsupported method. Request path: %s. HTTP method: %s", path, httpMethod);
			return APIGatewayV2HTTPResponse.builder()
					.withStatusCode(400)
					.withHeaders(Map.of("Content-Type", "application/json"))
					.withBody(gson.toJson(Map.of("message", message, "statusCode", 400)))
					.build();
		}
	}
}