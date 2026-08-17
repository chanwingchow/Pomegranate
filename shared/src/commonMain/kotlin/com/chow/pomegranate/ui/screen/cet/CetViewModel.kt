package com.chow.pomegranate.ui.screen.cet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.CetExamRepository
import com.chow.pomegranate.service.cet.model.CetExam
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * CET UI 状态。
 */
sealed interface CetUiState {
    /**
     * 加载中。
     */
    data object Loading : CetUiState

    /**
     * 加载成功。
     *
     * @param exam CET 考试
     */
    data class Success(
        val exam: CetExam,
        val pdfUiState: CetPdfUiState = CetPdfUiState.Idle,
    ) : CetUiState

    /**
     * 加载失败。
     *
     * @property throwable 异常
     */
    data class Error(val throwable: Throwable) : CetUiState

    /**
     * CET PDF UI 状态。
     */
    sealed interface CetPdfUiState {
        /**
         * 空闲。
         */
        data object Idle : CetPdfUiState

        /**
         * 加载中。
         */
        data object Loading : CetPdfUiState

        /**
         * 加载成功。
         *
         * @param pdfBytes PDF 内容
         */
        data class Success(val pdfBytes: ByteArray) : CetPdfUiState {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Success

                return pdfBytes.contentEquals(other.pdfBytes)
            }

            override fun hashCode(): Int {
                return pdfBytes.contentHashCode()
            }
        }

        /**
         * 加载失败。
         *
         * @property throwable 异常
         */
        data class Error(val throwable: Throwable) : CetPdfUiState
    }
}

@KoinViewModel
class CetViewModel(
    private val cetExamRepository: CetExamRepository,
) : ViewModel() {
    /** UI 状态 */
    val uiState: StateFlow<CetUiState>
        field = MutableStateFlow<CetUiState>(CetUiState.Loading)

    init {
        getCetExam()
    }

    /**
     * 获取 [CetExam]。
     */
    fun getCetExam() {
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                // 加载失败
                uiState.value = CetUiState.Error(throwable)
            },
        ) {
            val cetExam = cetExamRepository.getCetExam()

            // 加载成功
            uiState.value = CetUiState.Success(cetExam)
        }
    }

    /**
     * 获取 CET 考试安排 PDF 内容。
     */
    fun getCetExamPdf() {
        if (uiState.value !is CetUiState.Success) return

        val pdfUiState = (uiState.value as? CetUiState.Success)?.pdfUiState

        if (
            pdfUiState is CetUiState.CetPdfUiState.Loading ||
            pdfUiState is CetUiState.CetPdfUiState.Success
        ) {
            return
        }

        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                // 加载失败
                val current = uiState.value as? CetUiState.Success
                    ?: return@CoroutineExceptionHandler

                uiState.value = current.copy(
                    pdfUiState = CetUiState.CetPdfUiState.Error(throwable),
                )
            },
        ) {
            // 加载中
            uiState.value = CetUiState.Success(
                (uiState.value as CetUiState.Success).exam,
                CetUiState.CetPdfUiState.Loading,
            )

            val urlString = (uiState.value as CetUiState.Success).exam.pdfUrlString
            val pdf = cetExamRepository.getCetExamPdf(urlString)

            // 加载成功
            val latest = uiState.value as? CetUiState.Success ?: return@launch
            uiState.value = latest.copy(
                pdfUiState = CetUiState.CetPdfUiState.Success(pdf),
            )
        }
    }
}