export const mockNotes = [
    {
        id: 1,
        title: '我家的小可爱',
        content: '今天天气真好，带猫咪出来晒太阳。',
        coverImg: '/petmeetImage/cat/cat_1307503.jpg',
        authorAvatar: '/petmeetImage/cat/cat_32623335.jpg',
        authorName: '爱猫的Alice',
        likeCount: 120,
        createTime: '2023-10-01 10:00:00',
        images: ['/petmeetImage/cat/cat_1307503.jpg', '/petmeetImage/cat/cat_16618547.jpg'],
        productRelations: [
            { id: 101, name: '猫罐头', price: 12.5, img: '/petmeetImage/cat/cat_2071873.jpg' }
        ],
        comments: [
            { id: 1, userName: 'Bob', userAvatar: '/petmeetImage/dog/dog_1254140.jpg', content: '真可爱！', createTime: '2023-10-01 10:05:00' }
        ]
    },
    {
        id: 2,
        title: '狗狗的快乐生活',
        coverImg: '/petmeetImage/dog/dog_1108099.jpg',
        // 这里先统一用已经确认存在的图片资源，避免因为素材路径不稳定导致演示图失效。
        authorAvatar: '/petmeetImage/cat/cat_33034270.jpg',
        authorName: 'DogLover',
        likeCount: 45,
        createTime: '2023-09-28 14:00:00'
    },
    {
        id: 3,
        title: '新买的猫爬架',
        coverImg: '/petmeetImage/cat/cat_31161218.jpg',
        authorAvatar: '/petmeetImage/cat/cat_31440941.jpg',
        authorName: '铲屎官',
        likeCount: 88,
        createTime: '2023-10-02 09:30:00'
    },
    {
        id: 4,
        title: '呆萌瞬间',
        coverImg: '/petmeetImage/cat/cat_31440951.jpg',
        authorAvatar: '/petmeetImage/cat/cat_31440969.jpg',
        authorName: 'Mew',
        likeCount: 200,
        createTime: '2023-10-03 11:20:00'
    },
    {
        id: 5,
        title: '暗中观察',
        coverImg: '/petmeetImage/cat/cat_31440972.jpg',
        authorAvatar: '/petmeetImage/cat/cat_31440974.jpg',
        authorName: 'Watcher',
        likeCount: 56,
        createTime: '2023-10-03 15:45:00'
    },
    {
        id: 6,
        title: '睡觉觉',
        coverImg: '/petmeetImage/cat/cat_31440976.jpg',
        authorAvatar: '/petmeetImage/cat/cat_31440980.jpg',
        authorName: 'SleepyHead',
        likeCount: 302,
        createTime: '2023-10-04 20:10:00'
    }
]
