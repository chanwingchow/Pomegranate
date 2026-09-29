package com.chow.pomegranate.service.academic.affairs.api

import com.chow.pomegranate.service.academic.affairs.model.CourseSearchParam
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.LocalTestCredentials
import com.chow.pomegranate.session.CookieStorage
import com.chow.pomegranate.session.baseAcademicAffairsHttpClient
import com.chow.pomegranate.tool.ocr.OcrEngine
import com.chow.pomegranate.tool.ocr.createCaptchaOcrEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

/**
 * 教务系统测试。
 */
class AcademicAffairsTest {
    private lateinit var academicAffairs: AcademicAffairs
    private lateinit var captchaOcrEngine: OcrEngine
    private lateinit var username: String
    private lateinit var password: String

    @BeforeTest
    fun setUp() {
        // 使用不包含会话校验的客户端，测试中每次自行登录
        academicAffairs = AcademicAffairs(baseAcademicAffairsHttpClient(CookieStorage()))
        captchaOcrEngine = createCaptchaOcrEngine()

        // 没有配置账号时跳过，避免无凭据环境下以失败告终
        val credentials = LocalTestCredentials.academicAffairs ?: return

        username = credentials.userId
        password = credentials.password
    }

    /**
     * 登录测试。
     */
    @Test
    fun login() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {}
    }

    /**
     * 获取课表测试。
     */
    @Test
    fun getTimetable() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val semester = Semester.now()

            val timetable = academicAffairs.enrollment.getTimetable(semester)

            println(timetable)
        }
    }

    /**
     * 获取课程课表测试。
     */
    @Test
    fun getCourseTimetable() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val semester = Semester.now()

            val courseTimetable = academicAffairs.enrollment.getCourseTimetable(semester)

            println(courseTimetable)
        }
    }

    /**
     * 获取考试安排测试。
     */
    @Test
    fun getExamSchedule() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val semester = Semester.now()

            val examSchedule = academicAffairs.enrollment.getExamSchedule(semester)

            println(examSchedule)
        }
    }

    /**
     * 获取课程成绩测试。
     */
    @Test
    fun getCourseTranscript() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val courseTranscript = academicAffairs.enrollment.getCourseTranscript()

            println(courseTranscript)
        }
    }

    /**
     * 获取等级成绩测试。
     */
    @Test
    fun getLevelTranscript() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val levelTranscript = academicAffairs.enrollment.getLevelTranscript()

            println(levelTranscript)
        }
    }

    /**
     * 获取免听申请测试。
     */
    @Test
    fun getExemption() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val semester = Semester.now()

            val exemption = academicAffairs.enrollment.getExemption(semester)

            println(exemption)
        }
    }

    /**
     * 获取校历测试。
     */
    @Test
    fun getCampusCalendar() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val semester = Semester.now()

            val campusCalendar = academicAffairs.enrollment.getCampusCalendar(semester)

            println(campusCalendar)
        }
    }

    /**
     * 获取教师列表测试。
     */
    @Test
    fun getTeachers() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val teachers = academicAffairs.enrollment.getTeachers()

            println(teachers)
        }
    }

    /**
     * 获取教师测试。
     */
    @Test
    fun getTeacher() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val teachers = academicAffairs.enrollment.getTeachers()
            val id = teachers.items.first().id

            val teacher = academicAffairs.enrollment.getTeacher(id)

            println(teacher)
        }
    }

    /**
     * 获取学业计划测试。
     */
    @Test
    fun getAcademicPlan() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val academicPlan = academicAffairs.academic.getAcademicPlan()

            println(academicPlan)
        }
    }

    /**
     * 获取学业进度测试。
     */
    @Test
    fun getAcademicProgress() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val academicProgress = academicAffairs.academic.getAcademicProgress()

            println(academicProgress)
        }
    }

    /**
     * 获取学业预警测试。
     */
    @Test
    fun getAcademicAlert() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val academicAlert = academicAffairs.academic.getAcademicAlert()

            println(academicAlert)
        }
    }

    /**
     * 获取培养方案测试。
     */
    @Test
    fun getAcademicProgram() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val academicProgram = academicAffairs.academic.getAcademicProgram()

            println(academicProgram)
        }
    }

    /**
     * 获取毕业审核测试。
     */
    @Test
    fun getGraduationAudit() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithAcademicAffairsLogin {
            val graduationAudit = academicAffairs.academic.getGraduationAudit()

            println(graduationAudit)
        }
    }

    /**
     * 进入选课系统入口测试。
     */
    @Test
    fun enterCourseSystemSession() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithEnterCourseSystem {}
    }

    /**
     * 获取选课学分概览测试。
     */
    @Test
    fun getCreditSummary() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithEnterCourseSystem {
            val creditSummary = academicAffairs.courseSystem.getCreditSummary()

            println(creditSummary)
        }
    }

    /**
     * 获取已选课程测试。
     */
    @Test
    fun getSelectedCourses() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithEnterCourseSystem {
            val selectedCourses = academicAffairs.courseSystem.getSelectedCourses()

            println(selectedCourses)
        }
    }

    /**
     * 获取退课记录测试。
     */
    @Test
    fun getCourseDropLogs() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithEnterCourseSystem {
            val courseDropLogs = academicAffairs.courseSystem.getCourseDropLogs()

            println(courseDropLogs)
        }
    }

    /**
     * 搜索选课系统课程测试。
     */
    @Test
    fun searchCourse() = runBlocking {
        if (!hasAcademicAffairsCredentials()) return@runBlocking
        runWithEnterCourseSystem {
            val params = CourseSearchParam.General()
            val response = academicAffairs.courseSystem.searchCourses(params)

            println(response)
        }
    }

    /**
     * 在进入选课系统的情况下运行 [block]。
     */
    private suspend inline fun runWithEnterCourseSystem(block: () -> Unit) {
        runWithAcademicAffairsLogin {
            val entrances = academicAffairs.courseSystem.getEntrances()

            println(entrances)

            if (entrances.isNotEmpty()) {
                // 进入选课系统
                academicAffairs.courseSystem.enterCourseSystem(entrances.first().id)
                return block()
            }

            fail()
        }
    }

    /**
     * 在登录的情况下运行 [block]。
     */
    private suspend inline fun runWithAcademicAffairsLogin(block: () -> Unit) {
        var count = 5

        while (count-- > 0) {
            // 获取验证码
            val bytes = academicAffairs.auth.getCaptcha()

            // 识别验证码
            val captcha = captchaOcrEngine.recognize(bytes)

            // 登录
            val param = LoginParam(
                username = username,
                password = password,
                captcha = captcha,
            )
            val loginResult = academicAffairs.auth.login(param)

            when (loginResult) {
                is LoginResult.Fail.CaptchaError -> {
                    continue
                }

                is LoginResult.Fail.Unknow -> {
                    fail()
                }

                is LoginResult.Success -> {
                    return try {
                        block()
                    } finally {
                        academicAffairs.auth.logout()
                    }
                }
            }
        }

        fail()
    }
}

/**
 * 没有配置真实账号时跳过整个用例。
 *
 * 这些用例要连校园网并用真账号登录，CI 上必然失败；跳过才能让真实回归不被淹没。
 */
private fun hasAcademicAffairsCredentials(): Boolean =
    LocalTestCredentials.academicAffairs != null
