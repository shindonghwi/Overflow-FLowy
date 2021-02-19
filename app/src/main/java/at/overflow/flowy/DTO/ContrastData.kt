package at.overflow.flowy.DTO


/**
 * 고대비 색상값에 대한 정보
 *
 * removeBtnImage : 아이템 제거 이미지
 * contrastLeftImage : 고대비 1번 색상 // 흑색
 * contrastRightImage : 고대비 2번 색상 // 백색
 * contrastTextInfo : 고대비 색상 이름 // 흑/백
 * dragAndDropImage : 드래그 앤 드랍 이미지 - 현재는 사용 안하는 변수이다. - 현재 드래그 앤 드랍 방식은은 이템을 클릭해서 움직인다.
 *
 * */

class ContrastData(
    var removeBtnImage: Int?,
    var contrastLeftImage: Int?,
    var contrastRightImage: Int?,
    var contrastTextInfo: String,
    var dragAndDropImage: Int?
)