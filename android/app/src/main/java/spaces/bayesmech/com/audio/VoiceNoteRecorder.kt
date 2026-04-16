package spaces.bayesmech.com.audio

import android.content.Context
import android.media.MediaRecorder
import spaces.bayesmech.com.data.AudioNoteAttachment
import java.io.File

class VoiceNoteRecorder(
    private val context: Context,
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingStartedAtMs: Long? = null

    fun isRecording(): Boolean = mediaRecorder != null

    fun start() {
        if (mediaRecorder != null) {
            return
        }

        val file = File(
            context.cacheDir,
            "voice-note-${System.currentTimeMillis()}.m4a",
        )

        val recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        outputFile = file
        mediaRecorder = recorder
        recordingStartedAtMs = System.currentTimeMillis()
    }

    fun stop(): AudioNoteAttachment {
        val recorder = mediaRecorder ?: error("Recorder is not active")
        val file = outputFile ?: error("Recorder output file is missing")
        val startedAtMs = recordingStartedAtMs ?: System.currentTimeMillis()

        return try {
            recorder.stop()
            AudioNoteAttachment(
                filePath = file.absolutePath,
                durationSeconds = ((System.currentTimeMillis() - startedAtMs) / 1000L)
                    .toInt()
                    .coerceAtLeast(1),
            )
        } finally {
            recorder.reset()
            recorder.release()
            mediaRecorder = null
            outputFile = null
            recordingStartedAtMs = null
        }
    }

    fun cancel() {
        val file = outputFile
        runCatching {
            mediaRecorder?.stop()
        }
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
        outputFile = null
        recordingStartedAtMs = null
        if (file != null && file.exists()) {
            file.delete()
        }
    }
}
