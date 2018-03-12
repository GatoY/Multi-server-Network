import cv2
def savevideo(index):
    cap=cv2.VideoCapture(index)
    i=0
    while(True):
        ret,frame=cap.read()
        cv2.imshow('image',frame)
        cv2.imwrite(str(i)+'frame.png',frame)
        i+=1
        if cv2.waitKey(1)==ord('q'):
            break
    cap.release()
    cv2.destroyAllWindows()
