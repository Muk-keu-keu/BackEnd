package mukkeu.mukkeu.analysis.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mukkeu.mukkeu.analysis.app.AnalysisService;
import mukkeu.mukkeu.analysis.dto.AnalysisRequest;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse;
import mukkeu.mukkeu.user.app.UserContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 먹방요기 — 영상 분석 후 내 근처에서 시킬 수 있는 조합을 돌려준다.
 */
@RestController
@RequestMapping("/v1/analyses")
@RequiredArgsConstructor
public class AnalysisController {

	private final AnalysisService analysisService;
	private final UserContextService userContextService;

	/** 배달 좌표는 요청이 아니라 로그인한 사용자에게서 가져온다. */
	@ResponseStatus(HttpStatus.OK)
	@PostMapping
	public AnalysisResponse analyze(@RequestBody @Valid AnalysisRequest request) {
		return analysisService.analyze(userContextService.getCurrentUserId(), request);
	}
}
