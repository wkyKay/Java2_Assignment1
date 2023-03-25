import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * This is just a demo for you, please run it on JDK17 (some statements may be not allowed in lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1
    public Map<String, Integer> getPtcpCountByInst() {
        Map<String, Integer> a = courses.stream().collect(Collectors.groupingBy(course -> course.institution, Collectors.summingInt(course -> course.participants)));
        return a;
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> a = courses.stream()
                .collect(Collectors.groupingBy(course -> course.institution + "-" + course.subject, Collectors.summingInt(course -> course.participants)));
        List<Map.Entry<String, Integer>> list = new ArrayList<>(a.entrySet().stream().toList());
        list.sort((o1, o2) -> o2.getValue() - o1.getValue());
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Map<String, List<List<String>>> a = new HashMap<>();
        List<List<String>> b = new ArrayList<>();
        for (int i = 0; i < courses.size(); i++) {

            String[] ins = courses.get(i).instructors.split(", ");
            for (int j = 0; j < ins.length; j++) {
                int finalJ = j;
                b.add(courses.stream().filter(course -> course.instructors.equals(ins[finalJ])).map(course -> course.title).distinct().sorted().collect(Collectors.toList()));
                b.add(courses.stream().filter(course -> Arrays.asList(course.instructors.split(", ")).contains(ins[finalJ])&& !course.instructors.equals(ins[finalJ])).map(course -> course.title).distinct().sorted().collect(Collectors.toList()));
                a.put(ins[j], b);
                b = new ArrayList<>();
            }
        }
        return a;
    }

    //4
    public List<String> getCourses(int topK, String by) {
        List<String> a = null;
        Field field;
        Field[] fields = Course.class.getDeclaredFields();
        field = Arrays.stream(fields).filter(f -> f.getName().toLowerCase().contains(by.toLowerCase())).findFirst().orElse(null);
        if (field != null && field.getType() == double.class) {
            a = courses.stream().sorted(Comparator.comparingDouble(course -> {
                try {
                    return (double) field.get(course)*-1;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })).map(course -> (course.title)).distinct().limit(topK).toList();
        }else if(field != null && field.getType() == int.class){
            a = courses.stream().sorted(Comparator.comparingInt(course -> {
                try {
                    return (int)field.get(course)*-1;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })).map(course -> (course.title)).distinct().limit(topK).toList();
        }

        return a;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        List<String> a = courses.stream().filter(course -> course.subject.toLowerCase().contains(courseSubject.toLowerCase())
                && course.percentAudited >= percentAudited && course.totalHours <= totalCourseHours).map(course -> course.title).distinct().sorted().toList();
        return a;
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        Map<String, List<Course>> a = courses.stream().collect(Collectors.groupingBy(course -> course.number, Collectors.toList()));
        List<CourseForT6> true_courses = new ArrayList<>();
        for (Map.Entry<String, List<Course>> entry : a.entrySet()) {
            String key = entry.getKey();
            List<Course> value = entry.getValue();

            Course c = value.stream().max(Comparator.comparing(course -> course.launchDate)).orElse(null);
            assert c != null;
            String title = c.title;
            Optional<Date> launchdate = value.stream().map(course -> course.launchDate).max(Comparator.naturalOrder());
            OptionalDouble average_Median_age = OptionalDouble.of(value.stream().mapToDouble(course -> course.medianAge).average().orElse(0.0));
            OptionalDouble average_male= OptionalDouble.of(value.stream().mapToDouble(course -> course.percentMale).average().orElse(0.0));
            OptionalDouble average_degree = OptionalDouble.of(value.stream().mapToDouble(course -> course.percentDegree).average().orElse(0.0));
            true_courses.add(new CourseForT6(key, title, launchdate, average_Median_age, average_male, average_degree));
        }

        List<String> answer = true_courses.stream().sorted(Comparator.comparing(c -> c.title))
                .sorted(Comparator.comparingDouble(course -> course.getSimilarity(age,gender,isBachelorOrHigher)))
                .map(course->course.title).distinct().limit(10).toList();

        return answer;

    }

}
class CourseForT6{
    String number;
    String title;
    Optional<Date> launchdate;
    OptionalDouble average_Median_age;
    OptionalDouble average_male;
    OptionalDouble average_degree;
    public CourseForT6(String number, String title, Optional<Date> date, OptionalDouble average_Median_age, OptionalDouble average_male, OptionalDouble average_degree){
        this.number = number;
        this.title = title;
        this.launchdate = date;
        this.average_Median_age = average_Median_age;
        this.average_male = average_male;
        this.average_degree = average_degree;
    }

    //        similarity value= (age -average Median Age)^2 + (gender100 - average Male)^2 + (isBachelorOrHigher100
//                - average Bachelor's Degree or Higher)^2$
    public double getSimilarity(int age, int gender, int isBachelorOrHigher){
        double similarity = Math.pow(age-average_Median_age.getAsDouble(),2) + Math.pow(gender*100-average_male.getAsDouble(),2)
                + Math.pow(isBachelorOrHigher*100 - average_degree.getAsDouble(),2);
        return similarity;
    }
}
class Course{
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        this.instructors = instructors;
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }

    public boolean IfAmongInstructor(String name){
        String[]instr = this.instructors.split(", ");
        if(instr.length == 1){
            return false;
        }
        for (String s : instr) {
            if (s.equals(name)) {
                return true;
            }
        }
        return false;
    }


}